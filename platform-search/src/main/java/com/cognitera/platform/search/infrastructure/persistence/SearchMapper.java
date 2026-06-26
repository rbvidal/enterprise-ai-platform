package com.cognitera.platform.search.infrastructure.persistence;

import com.cognitera.platform.search.model.ChunkMetadata;
import com.cognitera.platform.search.model.ChunkPosition;
import com.cognitera.platform.search.model.DocumentChunk;
import com.cognitera.platform.search.model.MetadataFilter;

import java.util.UUID;
import java.util.stream.Collectors;

/** Static mappers for converting between {@link DocumentChunk} domain models and {@link DocumentChunkEntity} JPA entities. */
public final class SearchMapper {

    private SearchMapper() {
    }

    /** Converts a chunk entity to the domain model, including nested position and metadata. */
    public static DocumentChunk toModel(DocumentChunkEntity entity) {
        return new DocumentChunk(
                entity.getId(),
                entity.getDocumentId(),
                entity.getDocumentVersion(),
                entity.getChunkType(),
                entity.getText(),
                new ChunkPosition(
                        entity.getPageNumber(),
                        entity.getSectionIndex(),
                        entity.getChunkIndex(),
                        entity.getStartOffset(),
                        entity.getEndOffset()),
                new ChunkMetadata(
                        entity.getTitle(),
                        entity.getDocumentType(),
                        entity.getCategory(),
                        entity.getTags(),
                        entity.getSource(),
                        entity.getTenantId(),
                        entity.getDocumentCreatedAt(),
                        entity.getAttributes().stream()
                                .map(attribute -> new MetadataFilter(attribute.getKey(), attribute.getValue()))
                                .toList(),
                        entity.getEmbeddingReference()),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    /** Converts a domain chunk to a JPA entity, generating a UUID if the chunk ID is null. */
    public static DocumentChunkEntity toEntity(DocumentChunk chunk) {
        return new DocumentChunkEntity(
                chunk.id() == null ? UUID.randomUUID() : chunk.id(),
                chunk.documentId(),
                chunk.documentVersion(),
                chunk.type(),
                chunk.text(),
                chunk.position().pageNumber(),
                chunk.position().sectionIndex(),
                chunk.position().chunkIndex(),
                chunk.position().startOffset(),
                chunk.position().endOffset(),
                chunk.metadata().title(),
                chunk.metadata().documentType(),
                chunk.metadata().category(),
                chunk.metadata().tags(),
                chunk.metadata().source(),
                chunk.metadata().tenantId(),
                chunk.metadata().documentCreatedAt(),
                chunk.metadata().attributes().stream()
                        .map(attribute -> new MetadataAttributeEmbeddable(attribute.key(), attribute.value()))
                        .collect(Collectors.toUnmodifiableSet()),
                chunk.metadata().embeddingReference());
    }
}
