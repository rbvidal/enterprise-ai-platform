package com.cognitera.platform.search.application;

import com.cognitera.platform.search.api.CitationService;
import com.cognitera.platform.search.model.CitationReference;
import com.cognitera.platform.search.model.DocumentChunk;
import com.cognitera.platform.search.model.RetrievalCandidate;
import org.springframework.stereotype.Service;

/** Default implementation of {@link CitationService} that builds citations from chunks with a 240-char excerpt. */
@Service
public class DefaultCitationService implements CitationService {

    @Override
    public CitationReference citationFor(DocumentChunk chunk) {
        return new CitationReference(
                chunk.documentId(),
                chunk.id(),
                chunk.documentVersion(),
                chunk.metadata().title(),
                chunk.position().pageNumber(),
                chunk.position().startOffset(),
                chunk.position().endOffset(),
                excerpt(chunk.text()));
    }

    @Override
    public CitationReference citationFor(RetrievalCandidate candidate) {
        return candidate.citation();
    }

    private String excerpt(String text) {
        if (text == null || text.length() <= 240) {
            return text;
        }
        return text.substring(0, 240);
    }
}
