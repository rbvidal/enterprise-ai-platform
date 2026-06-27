package com.cognitera.platform.ai.model;

import java.util.List;

/**
 * Describes the capabilities of an AI model.
 * Used by the orchestration layer for intelligent model and provider selection.
 */
public record ModelCapability(
        String modelName,
        String provider,
        boolean supportsStreaming,
        boolean supportsVision,
        boolean supportsJson,
        boolean supportsToolCalling,
        boolean supportsEmbeddings,
        boolean supportsReasoning,
        int maxContextWindow,
        double recommendedTemperature,
        List<String> preferredUseCases,
        List<String> tags
) {
    public ModelCapability {
        preferredUseCases = preferredUseCases != null ? List.copyOf(preferredUseCases) : List.of();
        tags = tags != null ? List.copyOf(tags) : List.of();
    }

    /** Returns true if this model can fulfill the requested capability. */
    public boolean supports(CapabilityRequest request) {
        return switch (request) {
            case STREAMING -> supportsStreaming;
            case VISION -> supportsVision;
            case JSON_OUTPUT -> supportsJson;
            case TOOL_CALLING -> supportsToolCalling;
            case EMBEDDING -> supportsEmbeddings;
            case REASONING -> supportsReasoning;
            case CHAT -> true;
        };
    }

    /** Named capabilities that can be requested. */
    public enum CapabilityRequest {
        CHAT, STREAMING, VISION, JSON_OUTPUT, TOOL_CALLING, EMBEDDING, REASONING
    }
}
