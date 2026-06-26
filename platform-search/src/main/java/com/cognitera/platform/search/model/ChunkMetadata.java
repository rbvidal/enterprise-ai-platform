package com.cognitera.platform.search.model;

import com.cognitera.platform.document.model.DocumentType;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/** Metadata attached to a document chunk including title, type, tags, and embedding reference. */
public record ChunkMetadata(
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
    public ChunkMetadata {
        tags = tags == null ? Set.of() : Set.copyOf(tags);
        attributes = attributes == null ? List.of() : List.copyOf(attributes);
    }
}
