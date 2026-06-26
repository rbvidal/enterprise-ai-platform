package com.cognitera.platform.search.api;

import com.cognitera.platform.search.model.CitationReference;
import com.cognitera.platform.search.model.DocumentChunk;
import com.cognitera.platform.search.model.RetrievalCandidate;

/** Service for generating citation references from chunks and retrieval candidates. */
public interface CitationService {
    /** Builds a citation reference from a document chunk. */
    CitationReference citationFor(DocumentChunk chunk);

    /** Returns the citation embedded in a retrieval candidate. */
    CitationReference citationFor(RetrievalCandidate candidate);
}
