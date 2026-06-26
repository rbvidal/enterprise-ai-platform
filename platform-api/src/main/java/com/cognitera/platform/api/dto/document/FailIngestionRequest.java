package com.cognitera.platform.api.dto.document;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO to mark an ingestion job as failed with a reason.
 */
public record FailIngestionRequest(
        @NotBlank String reason
) {
}
