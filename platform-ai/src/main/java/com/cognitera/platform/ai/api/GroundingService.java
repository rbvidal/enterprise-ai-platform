package com.cognitera.platform.ai.api;

import com.cognitera.platform.ai.model.ReasonedAnswer;
import com.cognitera.platform.ai.model.RetrievalContext;

/**
 * Grounds a raw AI answer against retrieved evidence to produce a reasoned answer.
 */
public interface GroundingService {
    /**
     * Grounds a raw answer by reattributing sources, computing confidence, and producing a {@link ReasonedAnswer}.
     */
    ReasonedAnswer ground(String rawAnswer, RetrievalContext retrievalContext);
}
