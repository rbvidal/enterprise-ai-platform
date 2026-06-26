package com.cognitera.platform.api.dto.search;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for a metadata key-value filter.
 */
public record MetadataFilterRequest(
        @NotBlank String key,
        @NotBlank String value
) {
}
