package com.cognitera.platform.ai.api;

import com.cognitera.platform.ai.model.AiRequest;
import com.cognitera.platform.ai.model.PromptContext;
import com.cognitera.platform.ai.model.RetrievalContext;

/**
 * Assembles a {@link PromptContext} from an AI request and retrieval context.
 */
public interface ContextAssembler {
    /**
     * Assembles the full prompt context including objectives, hierarchy, dossier, and system instruction.
     */
    PromptContext assemble(AiRequest request, RetrievalContext retrievalContext);
}
