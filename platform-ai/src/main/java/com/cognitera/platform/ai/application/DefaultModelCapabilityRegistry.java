package com.cognitera.platform.ai.application;

import com.cognitera.platform.ai.api.ModelCapabilityRegistry;
import com.cognitera.platform.ai.model.ModelCapability;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory model capability registry seeded with known Ollama and OpenAI models.
 * In production, capabilities would be discovered dynamically or loaded from configuration.
 */
@Component
public class DefaultModelCapabilityRegistry implements ModelCapabilityRegistry {

    private final Map<String, ModelCapability> models = new ConcurrentHashMap<>();

    public DefaultModelCapabilityRegistry() {
        registerDefaults();
    }

    @Override
    public Optional<ModelCapability> get(String modelName) {
        ModelCapability exact = models.get(modelName);
        if (exact != null) return Optional.of(exact);
        return models.values().stream()
                .filter(m -> m.modelName().equalsIgnoreCase(modelName))
                .findFirst();
    }

    @Override
    public List<ModelCapability> findByProvider(String provider) {
        return models.values().stream()
                .filter(m -> m.provider().equalsIgnoreCase(provider))
                .toList();
    }

    @Override
    public List<ModelCapability> findByCapability(ModelCapability.CapabilityRequest capability) {
        return models.values().stream()
                .filter(m -> m.supports(capability))
                .toList();
    }

    @Override
    public List<ModelCapability> findByProviderAndCapability(String provider,
                                                              ModelCapability.CapabilityRequest capability) {
        return models.values().stream()
                .filter(m -> m.provider().equalsIgnoreCase(provider) && m.supports(capability))
                .toList();
    }

    @Override
    public List<ModelCapability> listAll() {
        return List.copyOf(models.values());
    }

    private void registerDefaults() {
        register(new ModelCapability("qwen2.5:14b", "ollama",
                false, false, false, false, false, false,
                32768, 0.7, List.of("general", "rag", "analysis", "summarization"),
                List.of("local", "small", "general-purpose")));
        register(new ModelCapability("qwen2.5:7b", "ollama",
                true, false, false, false, false, false,
                32768, 0.7, List.of("general", "rag", "chat"),
                List.of("local", "small", "fast")));
        register(new ModelCapability("llama3.2", "ollama",
                true, false, true, true, false, false,
                131072, 0.7, List.of("general", "rag", "tool-calling", "json"),
                List.of("local", "large-context")));
        register(new ModelCapability("nomic-embed-text", "ollama",
                false, false, false, false, true, false,
                8192, 1.0, List.of("embedding"),
                List.of("local", "embedding")));
        register(new ModelCapability("gpt-4o", "openai",
                true, true, true, true, false, false,
                128000, 0.7, List.of("general", "rag", "vision", "json", "tool-calling", "analysis"),
                List.of("cloud", "multimodal", "enterprise")));
        register(new ModelCapability("gpt-4o-mini", "openai",
                true, true, true, true, false, false,
                128000, 0.7, List.of("general", "chat", "json", "fast"),
                List.of("cloud", "multimodal", "cost-effective")));
    }

    private void register(ModelCapability capability) {
        models.put(capability.modelName(), capability);
    }
}
