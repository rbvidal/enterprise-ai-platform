package com.cognitera.platform.search.model;

/** A retrieval candidate with keyword/vector/ranking scores, confidence, provider, and citation. */
public record RetrievalCandidate(
        ChunkReference chunk,
        String text,
        double keywordScore,
        double vectorScore,
        double rankingScore,
        double confidenceScore,
        String provider,
        CitationReference citation
) {
}
