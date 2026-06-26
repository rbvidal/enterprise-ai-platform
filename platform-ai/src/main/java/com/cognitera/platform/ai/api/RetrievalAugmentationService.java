package com.cognitera.platform.ai.api;

import com.cognitera.platform.ai.model.AiRequest;
import com.cognitera.platform.ai.model.RetrievalContext;

/**
 * Performs retrieval-augmented generation by retrieving sources, authorities, hierarchy, and timeline.
 */
public interface RetrievalAugmentationService {
    /**
     * Retrieves the full augmented context for the given AI request.
     */
    RetrievalContext retrieve(AiRequest request);
}
