package com.cognitera.platform.search.api;

import com.cognitera.platform.document.model.DocumentType;
import com.cognitera.platform.search.model.ChunkType;
import com.cognitera.platform.search.model.MetadataFilter;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Command carrying all data needed to index a single document chunk. */
public record IndexChunkCommand(
        UUID documentId,
        int documentVersion,
        ChunkType chunkType,
        String text,
        Integer pageNumber,
        Integer sectionIndex,
        int chunkIndex,
        Integer startOffset,
        Integer endOffset,
        String title,
        DocumentType documentType,
        String category,
        Set<String> tags,
        String source,
        String tenantId,
        Instant documentCreatedAt,
        List<MetadataFilter> attributes,
        String embeddingReference
) {
}
