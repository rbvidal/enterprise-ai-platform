package com.cognitera.platform.document.application;

import com.cognitera.platform.document.api.DocumentIngestionProcessor;
import com.cognitera.platform.document.infrastructure.persistence.IngestionJobEntity;
import com.cognitera.platform.document.infrastructure.persistence.JpaIngestionJobEntityRepository;
import com.cognitera.platform.document.model.IngestionStatus;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Scheduled worker that polls for pending ingestion jobs and delegates to a {@link DocumentIngestionProcessor}. */
@Component
public class DocumentIngestionWorker {

    private static final String NO_PROCESSOR_CONFIGURED =
            "Document ingestion processor is not configured. The semantic-indexing module currently indexes source code only and is not wired to uploaded documents.";

    private final JpaIngestionJobEntityRepository ingestionJobs;
    private final DocumentService documentService;
    private final ObjectProvider<DocumentIngestionProcessor> processorProvider;

    public DocumentIngestionWorker(
            JpaIngestionJobEntityRepository ingestionJobs,
            DocumentService documentService,
            ObjectProvider<DocumentIngestionProcessor> processorProvider
    ) {
        this.ingestionJobs = ingestionJobs;
        this.documentService = documentService;
        this.processorProvider = processorProvider;
    }

    /** Polls the top 10 pending ingestion jobs and processes each one. */
    @Scheduled(fixedDelayString = "${platform.document.ingestion.poll-delay-ms:10000}")
    public void processPendingJobs() {
        ingestionJobs.findTop10ByStatusOrderByCreatedAtAsc(IngestionStatus.PENDING)
                .forEach(this::processJob);
    }

    private void processJob(IngestionJobEntity job) {
        try {
            documentService.startIngestion(job.getId(), job.getRequestedBy());
            DocumentIngestionProcessor processor = Optional.ofNullable(processorProvider.getIfAvailable())
                    .orElseThrow(() -> new IllegalStateException(NO_PROCESSOR_CONFIGURED));
            processor.ingest(job.getDocumentId());
            documentService.completeIngestion(job.getId(), job.getRequestedBy());
        } catch (Exception ex) {
            documentService.failIngestion(job.getId(), job.getRequestedBy(), failureReason(ex));
        }
    }

    private String failureReason(Exception ex) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? ex.getClass().getSimpleName() : message;
    }
}
