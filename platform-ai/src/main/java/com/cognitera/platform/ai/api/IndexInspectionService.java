package com.cognitera.platform.ai.api;

import com.cognitera.platform.ai.model.AiRequest;
import com.cognitera.platform.ai.model.AiResponse;

/**
 * Inspects the search index to answer questions about what content is available.
 */
public interface IndexInspectionService {
    /**
     * Inspects the index based on the request and returns a report of matching documents and references.
     */
    AiResponse inspect(AiRequest request);
}
