package com.cognitera.platform.ai.model;

/**
 * A multi-dimensional confidence profile with source, semantic, structural, completeness, and overall scores.
 */
public record ConfidenceProfile(
        double sourceConfidence,
        double semanticConfidence,
        double structuralConfidence,
        double completenessConfidence,
        double overallConfidence,
        String explanation
) {
    /**
     * Returns a zero-confidence placeholder profile.
     */
    public static ConfidenceProfile none() {
        return new ConfidenceProfile(0.0, 0.0, 0.0, 0.0, 0.0, "No assessment available");
    }
}
