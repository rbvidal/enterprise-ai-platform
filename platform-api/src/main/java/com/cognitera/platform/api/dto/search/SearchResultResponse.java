package com.cognitera.platform.api.dto.search;

import com.cognitera.platform.search.model.SearchResult;

/**
 * Response DTO for a single search result including chunk reference and scoring details.
 */
public record SearchResultResponse(
        ChunkReferenceResponse chunk,
        String text,
        double score,
        double confidenceScore,
        String provider,
        CitationReferenceResponse citation,
        double keywordScore,
        double vectorScore,
        double rerankScore,
        String intent,
        String retrievalStrategy
) {
    /**
     * Converts a {@code SearchResult} domain object into an API response DTO.
     */
    public static SearchResultResponse from(SearchResult result) {
        return new SearchResultResponse(
                ChunkReferenceResponse.from(result.chunk()),
                result.text(),
                result.score(),
                result.confidenceScore(),
                result.provider(),
                CitationReferenceResponse.from(result.citation()),
                result.keywordScore(),
                result.vectorScore(),
                result.rerankScore(),
                result.intent(),
                result.retrievalStrategy());
    }
}
