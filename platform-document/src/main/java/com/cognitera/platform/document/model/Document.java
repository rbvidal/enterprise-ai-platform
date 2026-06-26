package com.cognitera.platform.document.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Immutable domain model representing a document with metadata, status, and version history. */
public record Document(
        UUID id,
        String tenantId,
        DocumentMetadata metadata,
        DocumentStatus status,
        int currentVersion,
        String createdBy,
        String updatedBy,
        Instant createdAt,
        Instant updatedAt,
        List<DocumentVersion> versions
) {
    public Document {
        versions = versions == null ? List.of() : List.copyOf(versions);
    }
}
