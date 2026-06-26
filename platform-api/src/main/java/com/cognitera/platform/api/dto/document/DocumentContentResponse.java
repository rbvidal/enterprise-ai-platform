package com.cognitera.platform.api.dto.document;

import java.util.List;
import java.util.UUID;

/**
 * Response DTO for a document's full content including extracted text and chunk anchors.
 */
public record DocumentContentResponse(
        UUID documentId,
        String title,
        String type,
        int version,
        String text,
        List<ChunkAnchor> chunks
) {
    /**
     * Lightweight reference to a chunk position within the document text.
     */
    public record ChunkAnchor(
            UUID chunkId,
            int chunkIndex,
            Integer startOffset,
            Integer endOffset,
            String excerpt
    ) {}
}
