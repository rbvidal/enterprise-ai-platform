package com.cognitera.platform.api.dto.search;

import com.cognitera.platform.search.model.SearchResultPage;

import java.util.List;

/**
 * Paged response DTO for search results.
 */
public record SearchResultPageResponse(
        List<SearchResultResponse> results,
        int page,
        int size,
        long totalElements,
        int totalPages,
        String retrievalStrategy
) {
    /**
     * Converts a {@code SearchResultPage} domain object into an API response DTO.
     */
    public static SearchResultPageResponse from(SearchResultPage page) {
        return new SearchResultPageResponse(
                page.results().stream().map(SearchResultResponse::from).toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages(),
                page.retrievalStrategy());
    }
}
