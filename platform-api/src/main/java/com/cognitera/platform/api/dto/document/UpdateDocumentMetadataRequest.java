package com.cognitera.platform.api.dto.document;

import com.cognitera.platform.document.model.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

/**
 * Request DTO for updating the metadata of an existing document.
 */
public record UpdateDocumentMetadataRequest(
        @NotBlank String title,
        @NotNull DocumentType type,
        String category,
        Set<String> tags,
        String visibility
) {
}
