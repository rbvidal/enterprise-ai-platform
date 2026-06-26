package com.cognitera.platform.ai.application;

import com.cognitera.platform.ai.api.ModelProvider;
import com.cognitera.platform.ai.model.ModelCapabilities;
import org.springframework.stereotype.Component;

/**
 * Default model provider that resolves Ollama model capabilities from the requested model name.
 */
@Component
public class DefaultModelProvider implements ModelProvider {

    @Override
    public ModelCapabilities capabilities(String requestedModel) {
        String model = requestedModel == null || requestedModel.isBlank() ? "not-configured" : requestedModel.trim();
        String provider = "not-configured".equalsIgnoreCase(model) ? "abstract" : "ollama";
        return new ModelCapabilities(provider, model, 8192, true, true, true);
    }
}
