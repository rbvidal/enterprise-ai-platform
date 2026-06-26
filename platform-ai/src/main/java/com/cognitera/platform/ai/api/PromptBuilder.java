package com.cognitera.platform.ai.api;

import com.cognitera.platform.ai.model.PromptContext;

/**
 * Builds a full prompt string from a {@link PromptContext}.
 */
public interface PromptBuilder {
    /**
     * Builds a prompt string from the given context.
     */
    String build(PromptContext context);

    /**
     * Returns the prompt template version identifier.
     */
    String templateVersion();
}
