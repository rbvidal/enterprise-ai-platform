package com.cognitera.platform.api.dto.document;

import com.cognitera.platform.document.model.DocumentType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

/**
 * Request DTO for creating a new document.
 */
public record CreateDocumentRequest(
        @NotBlank String title,
        @NotNull DocumentType type,
        @NotBlank String fileName,
        @NotBlank String contentType,
        @Min(1) long sizeBytes,
        @NotBlank String storageProvider,
        @NotBlank String storageKey,
        String checksumSha256,
        String category,
        Set<String> tags,
        String visibility,
        String tenantId
) {
}
