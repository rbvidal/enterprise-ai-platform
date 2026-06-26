package com.cognitera.platform.api.dto.audit;

import com.cognitera.platform.audit.api.AuditEventPage;

import java.util.List;

/**
 * Paged response DTO for audit event listings.
 */
public record AuditEventPageResponse(
        List<AuditEventResponse> events,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    /**
     * Converts an {@code AuditEventPage} domain object into an API response DTO.
     */
    public static AuditEventPageResponse from(AuditEventPage page) {
        return new AuditEventPageResponse(
                page.events().stream().map(AuditEventResponse::from).toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages());
    }
}
