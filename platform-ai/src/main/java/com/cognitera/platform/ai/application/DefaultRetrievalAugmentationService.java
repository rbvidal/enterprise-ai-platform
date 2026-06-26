package com.cognitera.platform.ai.application;

import com.cognitera.platform.ai.api.*;
import com.cognitera.platform.ai.model.*;
import com.cognitera.platform.search.api.SearchFacade;
import com.cognitera.platform.search.model.SearchFilter;
import com.cognitera.platform.search.model.SearchMode;
import com.cognitera.platform.search.model.SearchQuery;
import com.cognitera.platform.search.model.SearchRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Performs retrieval-augmented generation by searching the document index
 * via {@link SearchFacade} and enriching results with grounding and analysis.
 */
@Service
public class DefaultRetrievalAugmentationService implements RetrievalAugmentationService {

    private static final Logger log = LoggerFactory.getLogger(DefaultRetrievalAugmentationService.class);

    private final SearchFacade searchFacade;
    private final ObjectiveAnalysisService objectiveAnalysisService;
    private final FindingHierarchyService findingHierarchyService;
    private final SourceOrchestrationService sourceOrchestrationService;
    private final SemanticCentralityService centralityService;
    private final CommunicationTimelineBuilder timelineBuilder;
    private final RetrievalOrchestrationService orchestrationService;
    private final AuthorityGroundingService authorityGroundingService;

    public DefaultRetrievalAugmentationService(
            SearchFacade searchFacade,
            ObjectiveAnalysisService objectiveAnalysisService,
            FindingHierarchyService findingHierarchyService,
            SourceOrchestrationService sourceOrchestrationService,
            SemanticCentralityService centralityService,
            CommunicationTimelineBuilder timelineBuilder,
            RetrievalOrchestrationService orchestrationService,
            AuthorityGroundingService authorityGroundingService) {
        this.searchFacade = searchFacade;
        this.objectiveAnalysisService = objectiveAnalysisService;
        this.findingHierarchyService = findingHierarchyService;
        this.sourceOrchestrationService = sourceOrchestrationService;
        this.centralityService = centralityService;
        this.timelineBuilder = timelineBuilder;
        this.orchestrationService = orchestrationService;
        this.authorityGroundingService = authorityGroundingService;
    }

    @Override
    public RetrievalContext retrieve(AiRequest request) {
        RetrievalScope scope = request.retrievalScope();

        RetrievalOrchestrationService.SearchDelegate searchDelegate = (q, maxResults) -> {
            var searchQuery = new SearchQuery(
                    q,
                    SearchMode.HYBRID,
                    new SearchFilter(null, null, null, null, null, null, null, null, List.of()),
                    new SearchRequestContext("system", null, null, null),
                    0,
                    Math.min(maxResults, 20));
            var page = searchFacade.search(searchQuery);
            return page.results().stream()
                    .map(r -> new SourceCitation(
                            r.chunk().documentId(),
                            r.chunk().chunkId(),
                            r.chunk().documentVersion(),
                            r.citation().title() != null ? r.citation().title() : (r.chunk().title() != null ? r.chunk().title() : ""),
                            r.chunk().position() != null ? r.chunk().position().pageNumber() : r.citation().pageNumber(),
                            r.chunk().position() != null ? r.chunk().position().startOffset() : r.citation().startOffset(),
                            r.chunk().position() != null ? r.chunk().position().endOffset() : r.citation().endOffset(),
                            r.citation().excerpt() != null ? r.citation().excerpt() : r.text(),
                            r.score(),
                            SourceCitation.classifyTier(r.score()),
                            SourceCitation.SourceType.FACTUAL))
                    .toList();
        };

        List<SourceCitation> initialSources = searchDelegate.search(request.question(), 20);

        SourceDossier dossier;
        String retrievalStrategy;

        if (scope == RetrievalScope.AUTHORITATIVE_ONLY) {
            dossier = new SourceDossier(Map.of(), List.of(), List.of(), 0.0, "AUTHORITATIVE_ONLY");
            retrievalStrategy = "AUTHORITATIVE_ONLY";
        } else {
            SourceDossier initialDossier = sourceOrchestrationService.buildDossier(initialSources, request.question());
            var quotaResult = orchestrationService.retrieveWithQuotas(
                    request.question(), initialSources, initialDossier, searchDelegate);
            dossier = quotaResult.dossier();
            retrievalStrategy = quotaResult.strategy();
        }

        AuthorityGroundingService.AuthorityGroundingResult authorityResult =
                authorityGroundingService.ground(request.question());

        List<AnalysisObjective> objectives = objectiveAnalysisService.classify(request.question());
        List<AuthorityReference> centralReferences = centralityService.filterCentral(
                authorityResult.references(), request.question(), authorityResult.concepts());
        FindingHierarchy hierarchy = findingHierarchyService.buildHierarchy(request.question(), objectives);
        var proceduralTimeline = timelineBuilder.build(request.question(), initialSources);

        log.info("RAG retrieval: {} initial sources, dossier coverage {:.0f}%",
                initialSources.size(), dossier.coverageScore() * 100);

        return new RetrievalContext(
                request.question(),
                retrievalStrategy,
                initialSources,
                centralReferences,
                hierarchy,
                dossier,
                proceduralTimeline);
    }
}
