package com.cognitera.platform.workspace.application;

import com.cognitera.platform.document.api.DocumentFacade;
import com.cognitera.platform.document.api.TextExtractionService;
import com.cognitera.platform.document.model.DocumentStatus;
import com.cognitera.platform.document.model.Document;
import com.cognitera.platform.workspace.api.*;
import com.cognitera.platform.workspace.infrastructure.persistence.*;
import com.cognitera.platform.workspace.model.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/** Core service managing workspace CRUD, document attachment, timeline events, phase transitions, and analysis. */
@Service
@Transactional
public class WorkspaceService {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceService.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final JpaWorkspaceRepository workspaceRepo;
    private final JpaWorkspaceDocumentLinkRepository docLinkRepo;
    private final JpaTimelineEventRepository timelineRepo;
    private final JpaWorkspaceStepRepository stepRepo;
    private final DocumentFacade documentFacade;
    private final TimelineExtractionService timelineExtractor;
    private final ObjectProvider<TextExtractionService> textExtractionService;
    private final WorkspaceOrchestrator orchestrator;

    public WorkspaceService(JpaWorkspaceRepository workspaceRepo,
                            JpaWorkspaceDocumentLinkRepository docLinkRepo,
                            JpaTimelineEventRepository timelineRepo,
                            JpaWorkspaceStepRepository stepRepo,
                            DocumentFacade documentFacade,
                            TimelineExtractionService timelineExtractor,
                            ObjectProvider<TextExtractionService> textExtractionService) {
        this.workspaceRepo = workspaceRepo;
        this.docLinkRepo = docLinkRepo;
        this.timelineRepo = timelineRepo;
        this.stepRepo = stepRepo;
        this.documentFacade = documentFacade;
        this.timelineExtractor = timelineExtractor;
        this.textExtractionService = textExtractionService;
        this.orchestrator = new WorkspaceOrchestrator();
    }

    /** Creates a new workspace from the given command, defaulting the name if blank. */
    public WorkspaceEntity createWorkspace(CreateWorkspaceCommand cmd) {
        String name = cmd.name() != null && !cmd.name().isBlank()
                ? cmd.name()
                : "WS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        WorkspaceEntity entity = new WorkspaceEntity(
                null, name, cmd.description(), cmd.workspaceType(), cmd.createdBy());
        WorkspaceEntity saved = workspaceRepo.save(entity);
        recordStep(saved.getId(), WorkspacePhase.SETUP, "Workspace setup completed");
        log.info("Created workspace {} (name: {})", saved.getId(), saved.getName());
        return saved;
    }

    /** Finds a workspace by its ID. */
    @Transactional(readOnly = true)
    public Optional<WorkspaceEntity> findById(String workspaceId) {
        return workspaceRepo.findById(workspaceId);
    }

    /** Finds workspaces owned by a given user. */
    @Transactional(readOnly = true)
    public List<WorkspaceEntity> findByOwner(String ownerId) {
        return workspaceRepo.findByOwnerId(ownerId);
    }

    /** Returns all workspaces. */
    @Transactional(readOnly = true)
    public List<WorkspaceEntity> findAll() {
        return workspaceRepo.findAll();
    }

    /** Sets the workspace phase explicitly to the given value. */
    public WorkspaceEntity setPhase(String workspaceId, WorkspacePhase phase) {
        WorkspaceEntity entity = workspaceRepo.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));
        entity.setPhase(phase);
        entity.setUpdatedAt(Instant.now());
        recordStep(workspaceId, phase, "Phase set to " + phase.name());
        return workspaceRepo.save(entity);
    }

    /** Advances the workspace to the next sequential phase. */
    public WorkspaceEntity advancePhase(String workspaceId) {
        WorkspaceEntity entity = workspaceRepo.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));
        WorkspacePhase current = entity.getPhase() != null ? entity.getPhase() : WorkspacePhase.SETUP;
        WorkspacePhase next = switch (current) {
            case SETUP -> WorkspacePhase.INGESTION;
            case INGESTION -> WorkspacePhase.ANALYSIS;
            case ANALYSIS -> WorkspacePhase.REVIEW;
            case REVIEW -> WorkspacePhase.COMPLETE;
            case COMPLETE -> WorkspacePhase.COMPLETE;
        };
        entity.setPhase(next);
        entity.setUpdatedAt(Instant.now());
        recordStep(workspaceId, next, "Advanced to " + next.name());
        return workspaceRepo.save(entity);
    }

    /** Moves the workspace back to the previous phase. */
    public WorkspaceEntity previousPhase(String workspaceId) {
        WorkspaceEntity entity = workspaceRepo.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));
        WorkspacePhase current = entity.getPhase() != null ? entity.getPhase() : WorkspacePhase.SETUP;
        WorkspacePhase prev = switch (current) {
            case COMPLETE -> WorkspacePhase.REVIEW;
            case REVIEW -> WorkspacePhase.ANALYSIS;
            case ANALYSIS -> WorkspacePhase.INGESTION;
            case INGESTION -> WorkspacePhase.SETUP;
            case SETUP -> WorkspacePhase.SETUP;
        };
        entity.setPhase(prev);
        entity.setUpdatedAt(Instant.now());
        recordStep(workspaceId, prev, "Returned to " + prev.name());
        return workspaceRepo.save(entity);
    }

    /** Merges the given data map into the workspace's phase data JSON. */
    public void updatePhaseData(String workspaceId, Map<String, Object> data) {
        WorkspaceEntity entity = workspaceRepo.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));
        Map<String, Object> existing = parsePhaseData(entity.getPhaseData());
        existing.putAll(data);
        try {
            entity.setPhaseData(mapper.writeValueAsString(existing));
        } catch (JsonProcessingException e) {
            entity.setPhaseData("{}");
        }
        entity.setUpdatedAt(Instant.now());
        workspaceRepo.save(entity);
    }

    /** Attaches a document to a workspace. */
    public WorkspaceDocumentLinkEntity attachDocument(AttachDocumentCommand cmd) {
        WorkspaceEntity entity = workspaceRepo.findById(cmd.workspaceId())
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + cmd.workspaceId()));
        String category = cmd.documentCategory() != null ? cmd.documentCategory() : "general";
        WorkspaceDocumentLinkEntity link = new WorkspaceDocumentLinkEntity(
                null, cmd.workspaceId(), cmd.documentId(), cmd.notes(),
                cmd.documentType(), category);
        WorkspaceDocumentLinkEntity saved = docLinkRepo.save(link);
        entity.setUpdatedAt(Instant.now());
        workspaceRepo.save(entity);
        return saved;
    }

    /** Returns all document links for a workspace. */
    @Transactional(readOnly = true)
    public List<WorkspaceDocumentLinkEntity> getWorkspaceDocuments(String workspaceId) {
        return docLinkRepo.findByWorkspaceId(workspaceId);
    }

    /** Adds a single timeline event to a workspace. */
    public TimelineEventEntity addTimelineEvent(String workspaceId, LocalDate eventDate, String title,
                                                  String description, TimelineEventType type,
                                                  String sourceDocumentId, double confidence, boolean aiGenerated) {
        TimelineEventEntity event = new TimelineEventEntity(
                null, workspaceId, eventDate, title, description, type, sourceDocumentId, confidence, aiGenerated);
        return timelineRepo.save(event);
    }

    /** Deletes existing timeline events and saves the given replacement list. */
    public void replaceTimeline(String workspaceId, List<TimelineEventEntity> events) {
        timelineRepo.deleteByWorkspaceId(workspaceId);
        events.forEach(e -> {
            e.setId(null);
            e.setWorkspaceId(workspaceId);
        });
        timelineRepo.saveAll(events);
    }

    /** Returns timeline events for a workspace ordered by event date. */
    @Transactional(readOnly = true)
    public List<TimelineEventEntity> getTimeline(String workspaceId) {
        return timelineRepo.findByWorkspaceIdOrderByEventDate(workspaceId);
    }

    /** Returns completed steps for a workspace ordered by completion time. */
    @Transactional(readOnly = true)
    public List<WorkspaceStepEntity> getCompletedSteps(String workspaceId) {
        return stepRepo.findByWorkspaceIdOrderByCompletedAt(workspaceId);
    }

    /** Analyzes all documents in a workspace, extracting timeline events via {@link TimelineExtractionService}. */
    @Transactional
    public AnalysisResult analyzeDocuments(String workspaceId) {
        List<WorkspaceDocumentLinkEntity> docLinks = docLinkRepo.findByWorkspaceId(workspaceId);
        if (docLinks.isEmpty()) {
            return new AnalysisResult("NO_DOCUMENTS", 0, 0, 0, new ArrayList<>());
        }

        List<TimelineExtractionService.DocInfo> docInfos = new ArrayList<>();
        int processing = 0;
        int ready = 0;

        for (WorkspaceDocumentLinkEntity link : docLinks) {
            try {
                UUID docUuid = UUID.fromString(link.getDocumentId());
                Document doc = documentFacade.getDocument(docUuid, "system");
                boolean isProcessing = doc.status() == DocumentStatus.INGESTION_PENDING
                        || doc.status() == DocumentStatus.INGESTING
                        || doc.status() == DocumentStatus.DRAFT;
                if (isProcessing) {
                    processing++;
                } else if (doc.status() == DocumentStatus.READY) {
                    ready++;
                }
                String text = null;
                com.cognitera.platform.document.model.DocumentVersion currentVersion = doc.versions().stream()
                        .filter(v -> v.versionNumber() == doc.currentVersion())
                        .findFirst().orElse(null);
                if (currentVersion != null) {
                    try {
                        TextExtractionService extractor = getTextExtractor();
                        if (extractor != null) {
                            text = extractor.extractText(doc.metadata().type(), currentVersion);
                        }
                    } catch (Exception e) {
                        log.debug("Text extraction skipped for doc {}: {}", doc.id(), e.getMessage());
                    }
                }
                docInfos.add(new TimelineExtractionService.DocInfo(
                        link.getDocumentId(), doc.status().name(), text));
            } catch (IllegalArgumentException e) {
                log.debug("Skipping doc link {}: {}", link.getDocumentId(), e.getMessage());
            }
        }

        if (processing > 0 && ready == 0 && docInfos.stream().allMatch(d -> d.text() == null)) {
            return new AnalysisResult("PROCESSING", docLinks.size(), processing, 0, new ArrayList<>());
        }

        // Remove previous AI-generated events before re-extracting
        List<TimelineEventEntity> existing = timelineRepo.findByWorkspaceIdOrderByEventDate(workspaceId);
        existing.stream().filter(TimelineEventEntity::isAiGenerated).forEach(e -> timelineRepo.delete(e));

        TimelineExtractionService.ExtractionResult result = timelineExtractor.extractFromDocuments(docInfos);

        List<String> createdEventIds = new ArrayList<>();
        for (TimelineExtractionService.ExtractedEvent ee : result.events()) {
            TimelineEventEntity event = addTimelineEvent(workspaceId, ee.eventDate(), ee.title(),
                    ee.description(), ee.eventType(), ee.sourceDocumentId(), ee.confidence(), true);
            createdEventIds.add(event.getId());
        }

        String status = result.hasProcessingDocs() ? "EXTRACTING" : "COMPLETED";
        return new AnalysisResult(status, docLinks.size(), result.docsProcessing(), result.events().size(), createdEventIds);
    }

    /** Analysis result containing status, document counts, extracted event count, and event IDs. */
    public record AnalysisResult(String status, int totalDocs, int docsProcessing,
                                  int eventsExtracted, List<String> eventIds) {
        /** Returns {@code true} when all documents are still processing. */
        public boolean isProcessing() { return "PROCESSING".equals(status); }
        /** Returns {@code true} when extraction is still in progress. */
        public boolean isExtracting() { return "EXTRACTING".equals(status); }
        /** Returns {@code true} when analysis completed successfully. */
        public boolean isCompleted() { return "COMPLETED".equals(status); }
        /** Returns {@code true} when the workspace has no documents. */
        public boolean isNoDocuments() { return "NO_DOCUMENTS".equals(status); }
    }

    private TextExtractionService getTextExtractor() {
        return textExtractionService.getIfAvailable();
    }

    /** Converts a workspace entity to its full DTO representation including documents and timeline. */
    @Transactional(readOnly = true)
    public WorkspaceDto toDto(WorkspaceEntity entity) {
        List<WorkspaceDocumentLinkEntity> docLinks = docLinkRepo.findByWorkspaceId(entity.getId());
        List<TimelineEventEntity> timeline = timelineRepo.findByWorkspaceIdOrderByEventDate(entity.getId());

        return new WorkspaceDto(
                entity.getId(), entity.getName(), entity.getDescription(),
                entity.getWorkspaceType(), entity.getStatus(), entity.getPhase(),
                entity.getOwnerId(), parsePhaseData(entity.getPhaseData()),
                docLinks.stream().map(this::toDocDto).collect(Collectors.toList()),
                timeline.stream().map(this::toTimelineDto).collect(Collectors.toList()),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private WorkspaceDocumentDto toDocDto(WorkspaceDocumentLinkEntity link) {
        return new WorkspaceDocumentDto(
                link.getId(), link.getWorkspaceId(), link.getDocumentId(), link.getDocumentName(),
                link.getDocumentType(), link.getDocumentCategory(),
                parsePhaseData(link.getExtractedMetadata()), link.getUploadedAt());
    }

    private TimelineEventDto toTimelineDto(TimelineEventEntity e) {
        return new TimelineEventDto(
                e.getId(), e.getWorkspaceId(), e.getEventDate(), e.getTitle(), e.getDescription(),
                e.getEventType(), e.getSourceDocumentId(), e.getConfidence(), e.isAiGenerated());
    }

    private void recordStep(String workspaceId, WorkspacePhase phase, String stepName) {
        WorkspaceStepEntity step = new WorkspaceStepEntity(null, workspaceId, phase, stepName, "COMPLETED");
        stepRepo.save(step);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parsePhaseData(String json) {
        if (json == null || json.isBlank() || "{}".equals(json)) return new LinkedHashMap<>();
        try {
            return mapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (JsonProcessingException e) {
            return new LinkedHashMap<>();
        }
    }
}
