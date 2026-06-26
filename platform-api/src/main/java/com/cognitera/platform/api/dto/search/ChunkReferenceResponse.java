package com.cognitera.platform.api.dto.search;

import com.cognitera.platform.search.model.ChunkReference;

import java.util.UUID;

/**
 * Response DTO for a chunk reference including the parent document identifier and title.
 */
public record ChunkReferenceResponse(
        UUID chunkId,
        UUID documentId,
        int documentVersion,
        String title,
        ChunkPositionResponse position
) {
    /**
     * Converts a {@code ChunkReference} domain object into an API response DTO.
     */
    public static ChunkReferenceResponse from(ChunkReference reference) {
        return new ChunkReferenceResponse(
                reference.chunkId(),
                reference.documentId(),
                reference.documentVersion(),
                reference.title(),
                reference.position() != null
                        ? ChunkPositionResponse.from(reference.position())
                        : new ChunkPositionResponse(null, null, 0, null, null));
    }
}
