package com.cognitera.platform.api.dto.search;

import com.cognitera.platform.search.model.ChunkPosition;

/**
 * Response DTO for a chunk's positional reference within a document.
 */
public record ChunkPositionResponse(
        Integer pageNumber,
        Integer sectionIndex,
        int chunkIndex,
        Integer startOffset,
        Integer endOffset
) {
    /**
     * Converts a {@code ChunkPosition} domain object into an API response DTO.
     */
    public static ChunkPositionResponse from(ChunkPosition position) {
        return new ChunkPositionResponse(
                position.pageNumber(),
                position.sectionIndex(),
                position.chunkIndex(),
                position.startOffset(),
                position.endOffset());
    }
}
