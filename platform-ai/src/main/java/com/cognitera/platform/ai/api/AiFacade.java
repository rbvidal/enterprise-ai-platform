package com.cognitera.platform.ai.api;

import com.cognitera.platform.ai.model.AiRequest;
import com.cognitera.platform.ai.model.AiResponse;

/**
 * The top-level facade for answering AI questions.
 */
public interface AiFacade {
    /** Answers a question given an {@code AiRequest} and returns an {@code AiResponse}. */
    AiResponse answer(AiRequest request);
}
