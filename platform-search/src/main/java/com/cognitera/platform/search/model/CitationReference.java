package com.cognitera.platform.search.model;

import java.util.UUID;

/** Reference pointing to a document/chunk location for citation purposes. */
public record CitationReference(
        UUID documentId,
        UUID chunkId,
        int documentVersion,
        String title,
        Integer pageNumber,
        Integer startOffset,
        Integer endOffset,
        String excerpt
) {
}
