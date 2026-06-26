package com.cognitera.platform.api.dto.document;

import com.cognitera.platform.document.model.DocumentIngestionJob;
import com.cognitera.platform.document.model.IngestionStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for a single document ingestion job.
 */
public record IngestionJobResponse(
        UUID id,
        UUID documentId,
        IngestionStatus status,
        String sourceType,
        String requestedBy,
        String tenantId,
        String failureReason,
        Instant createdAt,
        Instant updatedAt,
        Instant completedAt,
        long sequenceNumber
) {
    /**
     * Converts a {@code DocumentIngestionJob} domain object into an API response DTO.
     */
    public static IngestionJobResponse from(DocumentIngestionJob job) {
        return new IngestionJobResponse(
                job.id(),
                job.documentId(),
                job.status(),
                job.sourceType(),
                job.requestedBy(),
                job.tenantId(),
                job.failureReason(),
                job.createdAt(),
                job.updatedAt(),
                job.completedAt(),
                job.sequenceNumber());
    }
}
