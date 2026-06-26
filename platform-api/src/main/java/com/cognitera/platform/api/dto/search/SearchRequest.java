package com.cognitera.platform.api.dto.search;

import com.cognitera.platform.document.model.DocumentType;
import com.cognitera.platform.search.model.SearchMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Request DTO for performing a document search with optional filters.
 */
public record SearchRequest(
        @NotBlank String query,
        SearchMode mode,
        Set<UUID> documentIds,
        DocumentType documentType,
        String category,
        String tag,
        String source,
        String tenantId,
        Instant createdFrom,
        Instant createdTo,
        @Valid List<MetadataFilterRequest> metadata,
        Integer page,
        Integer size
) {
}
