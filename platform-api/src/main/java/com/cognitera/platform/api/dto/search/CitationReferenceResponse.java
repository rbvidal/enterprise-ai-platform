package com.cognitera.platform.api.dto.search;

import com.cognitera.platform.search.model.CitationReference;

import java.util.UUID;

/**
 * Response DTO for a citation reference linking a search result back to a document chunk.
 */
public record CitationReferenceResponse(
        UUID documentId,
        UUID chunkId,
        int documentVersion,
        String title,
        Integer pageNumber,
        Integer startOffset,
        Integer endOffset,
        String excerpt
) {
    /**
     * Converts a {@code CitationReference} domain object into an API response DTO.
     */
    public static CitationReferenceResponse from(CitationReference citation) {
        return new CitationReferenceResponse(
                citation.documentId(),
                citation.chunkId(),
                citation.documentVersion(),
                citation.title(),
                citation.pageNumber(),
                citation.startOffset(),
                citation.endOffset(),
                citation.excerpt());
    }
}
