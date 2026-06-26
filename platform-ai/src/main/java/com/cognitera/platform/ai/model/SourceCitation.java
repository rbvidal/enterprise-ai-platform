package com.cognitera.platform.ai.model;

import java.util.UUID;

/**
 * A citation to a source document chunk with metadata, confidence scoring, and tier classification.
 */
public record SourceCitation(
        UUID documentId,
        UUID chunkId,
        int documentVersion,
        String title,
        Integer pageNumber,
        Integer startOffset,
        Integer endOffset,
        String excerpt,
        double confidenceScore,
        SourceTier tier,
        SourceType sourceType
) {
    public enum SourceTier { PRIMARY, SUPPORTING, BACKGROUND }
    public enum SourceType { FACTUAL, AUTHORITATIVE, UNKNOWN }

    public SourceCitation(
            UUID documentId, UUID chunkId, int documentVersion, String title,
            Integer pageNumber, Integer startOffset, Integer endOffset,
            String excerpt, double confidenceScore, SourceTier tier) {
        this(documentId, chunkId, documentVersion, title,
             pageNumber, startOffset, endOffset, excerpt,
             confidenceScore, tier, SourceType.UNKNOWN);
    }

    /**
     * Classifies a source into a tier based on an absolute confidence score.
     */
    public static SourceTier classifyTier(double score) {
        if (score >= 0.6) return SourceTier.PRIMARY;
        if (score >= 0.3) return SourceTier.SUPPORTING;
        return SourceTier.BACKGROUND;
    }

    /**
     * Classifies a source into a tier relative to the maximum score in a batch.
     */
    public static SourceTier classifyTierRelative(double score, double maxScore) {
        if (maxScore <= 0) return SourceTier.BACKGROUND;
        double ratio = score / maxScore;
        if (ratio >= 0.85 || score >= 0.5) return SourceTier.PRIMARY;
        if (ratio >= 0.50 || score >= 0.2) return SourceTier.SUPPORTING;
        return SourceTier.BACKGROUND;
    }
}
