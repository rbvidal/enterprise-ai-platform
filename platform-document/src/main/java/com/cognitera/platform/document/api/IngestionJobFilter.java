package com.cognitera.platform.document.api;

import com.cognitera.platform.document.model.IngestionStatus;

import java.util.UUID;

/** Filter criteria for querying ingestion jobs by document, status, and tenant. */
public record IngestionJobFilter(
        UUID documentId,
        IngestionStatus status,
        String tenantId,
        int page,
        int size
) {
}
