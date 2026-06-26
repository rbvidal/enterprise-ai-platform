package com.cognitera.platform.api.dto.document;

import com.cognitera.platform.document.model.DocumentStatus;
import com.cognitera.platform.document.model.DocumentType;
import com.cognitera.platform.document.model.Document;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Response DTO for a document including its metadata and version history.
 */
public record DocumentResponse(
        UUID id,
        String tenantId,
        String title,
        DocumentType type,
        DocumentStatus status,
        String category,
        Set<String> tags,
        String visibility,
        int currentVersion,
        String createdBy,
        String updatedBy,
        Instant createdAt,
        Instant updatedAt,
        List<DocumentVersionResponse> versions
) {
    /**
     * Converts a {@code Document} domain object into an API response DTO.
     */
    public static DocumentResponse from(Document document) {
        return new DocumentResponse(
                document.id(),
                document.tenantId(),
                document.metadata().title(),
                document.metadata().type(),
                document.status(),
                document.metadata().category(),
                document.metadata().tags(),
                document.metadata().visibility(),
                document.currentVersion(),
                document.createdBy(),
                document.updatedBy(),
                document.createdAt(),
                document.updatedAt(),
                document.versions().stream().map(DocumentVersionResponse::from).toList());
    }
}
