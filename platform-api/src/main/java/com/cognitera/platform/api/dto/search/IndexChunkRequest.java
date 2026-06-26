package com.cognitera.platform.api.dto.search;

import com.cognitera.platform.document.model.DocumentType;
import com.cognitera.platform.search.model.ChunkType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Request DTO for indexing a text chunk for search.
 */
public record IndexChunkRequest(
        @NotNull UUID documentId,
        @Min(1) int documentVersion,
        ChunkType chunkType,
        @NotBlank String text,
        Integer pageNumber,
        Integer sectionIndex,
        int chunkIndex,
        Integer startOffset,
        Integer endOffset,
        @NotBlank String title,
        @NotNull DocumentType documentType,
        String category,
        Set<String> tags,
        String source,
        String tenantId,
        Instant documentCreatedAt,
        @Valid List<MetadataFilterRequest> attributes,
        String embeddingReference
) {
}
