package com.cognitera.platform.ai.api;

import com.cognitera.platform.ai.model.RetrievalOrchestrationResult;
import com.cognitera.platform.ai.model.AiRequest;

/**
 * Top-level orchestrator for the retrieval pipeline.
 * Decides which retrieval strategy to use based on query intent,
 * executes the appropriate retriever pipeline, and returns fused results
 * with full explainability metadata.
 */
public interface RetrievalOrchestrator {

    /**
     * Orchestrates retrieval for the given AI request.
     * Intent → Strategy → Retrieval → Fusion → Reranking → Result.
     */
    RetrievalOrchestrationResult orchestrate(AiRequest request);
}
