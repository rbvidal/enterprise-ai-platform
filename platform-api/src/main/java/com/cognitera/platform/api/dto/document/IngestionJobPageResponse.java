package com.cognitera.platform.api.dto.document;

import com.cognitera.platform.document.api.IngestionJobPage;

import java.util.List;

/**
 * Paged response DTO for ingestion job listings.
 */
public record IngestionJobPageResponse(
        List<IngestionJobResponse> jobs,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    /**
     * Converts an {@code IngestionJobPage} domain object into an API response DTO.
     */
    public static IngestionJobPageResponse from(IngestionJobPage page) {
        return new IngestionJobPageResponse(
                page.jobs().stream().map(IngestionJobResponse::from).toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages());
    }
}
