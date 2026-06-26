package com.cognitera.platform.api.dto.search;

import com.cognitera.platform.document.model.DocumentType;
import com.cognitera.platform.search.model.ChunkType;
import com.cognitera.platform.search.model.DocumentChunk;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Response DTO for a document chunk including its text, metadata, and position.
 */
public record DocumentChunkResponse(
        UUID id,
        UUID documentId,
        int documentVersion,
        ChunkType type,
        String text,
        ChunkPositionResponse position,
        String title,
        DocumentType documentType,
        String category,
        Set<String> tags,
        String source,
        String tenantId,
        Instant documentCreatedAt,
        List<MetadataFilterRequest> attributes,
        String embeddingReference,
        Instant createdAt,
        Instant updatedAt
) {
    /**
     * Converts a {@code DocumentChunk} domain object into an API response DTO.
     */
    public static DocumentChunkResponse from(DocumentChunk chunk) {
        return new DocumentChunkResponse(
                chunk.id(),
                chunk.documentId(),
                chunk.documentVersion(),
                chunk.type(),
                chunk.text(),
                ChunkPositionResponse.from(chunk.position()),
                chunk.metadata().title(),
                chunk.metadata().documentType(),
                chunk.metadata().category(),
                chunk.metadata().tags(),
                chunk.metadata().source(),
                chunk.metadata().tenantId(),
                chunk.metadata().documentCreatedAt(),
                chunk.metadata().attributes().stream()
                        .map(attribute -> new MetadataFilterRequest(attribute.key(), attribute.value()))
                        .toList(),
                chunk.metadata().embeddingReference(),
                chunk.createdAt(),
                chunk.updatedAt());
    }
}
