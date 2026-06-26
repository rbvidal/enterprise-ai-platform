package com.cognitera.platform.api.dto.document;

import com.cognitera.platform.document.api.DocumentPage;

import java.util.List;

/**
 * Paged response DTO for document listings.
 */
public record DocumentPageResponse(
        List<DocumentResponse> documents,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    /**
     * Converts a {@code DocumentPage} domain object into an API response DTO.
     */
    public static DocumentPageResponse from(DocumentPage page) {
        return new DocumentPageResponse(
                page.documents().stream().map(DocumentResponse::from).toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages());
    }
}
