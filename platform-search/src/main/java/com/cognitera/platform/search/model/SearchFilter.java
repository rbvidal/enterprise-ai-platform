package com.cognitera.platform.search.model;

import com.cognitera.platform.document.model.DocumentType;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Comprehensive filter for search operations including document type, dates, tags, and metadata. */
public record SearchFilter(
        Set<UUID> documentIds,
        DocumentType documentType,
        String category,
        String tag,
        String source,
        String tenantId,
        Instant createdFrom,
        Instant createdTo,
        List<MetadataFilter> metadata
) {
    public SearchFilter {
        documentIds = documentIds == null ? Set.of() : Set.copyOf(documentIds);
        metadata = metadata == null ? List.of() : List.copyOf(metadata);
    }
}
