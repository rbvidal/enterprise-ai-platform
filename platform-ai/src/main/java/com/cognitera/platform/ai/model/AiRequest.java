package com.cognitera.platform.ai.model;

import java.util.UUID;

/**
 * Platform-agnostic search filter placeholder. Uses an untyped filter bag so
 * this module can operate without a dependency on the search layer.
 */
public record AiRequest(
        String question,
        String model,
        Object searchFilter,
        AiConversationContext context,
        int maxRetrievalResults,
        RetrievalScope retrievalScope,
        UUID workspaceId
) {
    public AiRequest {
        if (retrievalScope == null) retrievalScope = RetrievalScope.HYBRID;
    }

    public AiRequest(String question, String model, Object searchFilter,
                     AiConversationContext context, int maxRetrievalResults) {
        this(question, model, searchFilter, context, maxRetrievalResults,
                RetrievalScope.HYBRID, null);
    }
}
