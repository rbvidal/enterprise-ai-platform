package com.cognitera.platform.api.dto.document;

import com.cognitera.platform.document.model.DocumentVersion;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for a single version of a document.
 */
public record DocumentVersionResponse(
        UUID id,
        int versionNumber,
        String fileName,
        String contentType,
        long sizeBytes,
        String storageProvider,
        String storageKey,
        String checksumSha256,
        String createdBy,
        Instant createdAt
) {
    /**
     * Converts a {@code DocumentVersion} domain object into an API response DTO.
     */
    public static DocumentVersionResponse from(DocumentVersion version) {
        return new DocumentVersionResponse(
                version.id(),
                version.versionNumber(),
                version.fileName(),
                version.contentType(),
                version.sizeBytes(),
                version.storageProvider(),
                version.storageKey(),
                version.checksumSha256(),
                version.createdBy(),
                version.createdAt());
    }
}
