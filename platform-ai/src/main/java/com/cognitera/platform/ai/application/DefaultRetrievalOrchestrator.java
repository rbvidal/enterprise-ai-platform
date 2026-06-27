package com.cognitera.platform.ai.application;

import com.cognitera.platform.ai.api.*;
import com.cognitera.platform.ai.model.*;
import com.cognitera.platform.search.api.SearchFacade;
import com.cognitera.platform.search.model.SearchMode;
import com.cognitera.platform.search.model.SearchQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;

/**
 * Default retrieval orchestrator — intent-to-result pipeline with full explainability.
 *
 * <p>Pipeline: Intent Classification → Strategy Selection → Search Execution →
 * Result Assembly with explainability metadata.
 *
 * <p>Strategy selection is deterministic and explainable:
 * <ul>
 *   <li>INDEX_INSPECTION, CORPUS_DISCOVERY → KEYWORD</li>
 *   <li>GENERAL, CONTRACT, FINANCE, COMPLIANCE → HYBRID</li>
 *   <li>PROCEDURE, COMMUNICATION → SEMANTIC</li>
 * </ul>
 */
@Service
public class DefaultRetrievalOrchestrator implements RetrievalOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(DefaultRetrievalOrchestrator.class);

    private final SearchFacade searchFacade;
    private final QueryIntentClassifier intentClassifier;
    private final PromptRegistry promptRegistry;
    private final EvaluationService evaluationService;

    public DefaultRetrievalOrchestrator(SearchFacade searchFacade,
                                         QueryIntentClassifier intentClassifier,
                                         PromptRegistry promptRegistry,
                                         EvaluationService evaluationService) {
        this.searchFacade = searchFacade;
        this.intentClassifier = intentClassifier;
        this.promptRegistry = promptRegistry;
        this.evaluationService = evaluationService;
    }

    @Override
    public RetrievalOrchestrationResult orchestrate(AiRequest request) {
        Instant start = Instant.now();
        var trace = new ArrayList<String>();
        trace.add("Orchestration started");

        QueryIntent intent = intentClassifier.classify(request.question());
        trace.add("Intent: " + intent);

        SearchMode mode = selectMode(intent);
        trace.add("Mode: " + mode);

        var searchQuery = new SearchQuery(request.question(), mode, null, null, 0, 20);
        var page = searchFacade.search(searchQuery);
        trace.add("Results: " + page.results().size());

        PromptTemplate template = promptRegistry.getLatest("rag-answer").orElse(null);
        String promptId = template != null ? template.getQualifiedId() : "rag-answer/v1";
        int promptVersion = template != null ? template.getVersion() : 1;

        Instant end = Instant.now();
        trace.add("Completed in " + (end.toEpochMilli() - start.toEpochMilli()) + "ms");

        // Run evaluation on the retrieval quality
        var evalResult = evaluationService.evaluate(
                request.question(), "", page.results().toString());
        trace.add("Evaluation: grounding=" + String.format("%.2f", evalResult.groundingScore())
                + " faithfulness=" + String.format("%.2f", evalResult.faithfulness()));

        return RetrievalOrchestrationResult.builder()
                .intent(intent.name())
                .selectedStrategy(mode.name())
                .promptTemplateId(promptId)
                .promptTemplateVersion(promptVersion)
                .modelName(request.model() != null ? request.model() : "default")
                .retrievalStartedAt(start)
                .retrievalCompletedAt(end)
                .totalChunkCount(page.results().size())
                .totalSourceCount(page.results().size())
                .fusionMethod(mode == SearchMode.HYBRID || mode == SearchMode.HYBRID_GRAPH
                        ? "weighted-linear-fusion" : "single-source")
                .rerankingApplied(true)
                .rerankingProvider("ollama-cross-encoder")
                .traceLog(trace)
                .build();
    }

    private SearchMode selectMode(QueryIntent intent) {
        return switch (intent) {
            case INDEX_INSPECTION, CORPUS_DISCOVERY -> SearchMode.KEYWORD;
            case QUESTION_ANSWERING, WORKSPACE_ANALYSIS, SOURCE_ANALYSIS -> SearchMode.HYBRID;
            case DOCUMENT_RESEARCH, DOCUMENT_LOOKUP -> SearchMode.SEMANTIC;
        };
    }
}
