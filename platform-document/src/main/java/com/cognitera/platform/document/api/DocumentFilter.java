package com.cognitera.platform.document.api;

import com.cognitera.platform.document.model.DocumentStatus;
import com.cognitera.platform.document.model.DocumentType;

import java.time.Instant;

/** Filter criteria for querying documents by status, type, category, tag, tenant, and date range. */
public record DocumentFilter(
        DocumentStatus status,
        DocumentType type,
        String category,
        String tag,
        String tenantId,
        Instant createdFrom,
        Instant createdTo,
        int page,
        int size
) {
}
