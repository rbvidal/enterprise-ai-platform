package com.cognitera.platform.api.dto.document;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for adding a new version to an existing document.
 */
public record AddDocumentVersionRequest(
        @NotBlank String fileName,
        @NotBlank String contentType,
        @Min(1) long sizeBytes,
        @NotBlank String storageProvider,
        @NotBlank String storageKey,
        String checksumSha256
) {
}
