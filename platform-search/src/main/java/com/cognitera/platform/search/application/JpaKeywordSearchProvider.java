package com.cognitera.platform.search.application;

import com.cognitera.platform.search.api.CitationService;
import com.cognitera.platform.search.api.KeywordSearchProvider;
import com.cognitera.platform.search.infrastructure.persistence.DocumentChunkSpecifications;
import com.cognitera.platform.search.infrastructure.persistence.JpaDocumentChunkRepository;
import com.cognitera.platform.search.infrastructure.persistence.SearchMapper;
import com.cognitera.platform.search.model.ChunkReference;
import com.cognitera.platform.search.model.DocumentChunk;
import com.cognitera.platform.search.model.RetrievalCandidate;
import com.cognitera.platform.search.model.SearchQuery;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;

/** JPA-based keyword search provider that queries chunks by text matching with TF-based scoring. */
@Component
public class JpaKeywordSearchProvider implements KeywordSearchProvider {

    private final JpaDocumentChunkRepository chunks;
    private final CitationService citationService;

    public JpaKeywordSearchProvider(JpaDocumentChunkRepository chunks, CitationService citationService) {
        this.chunks = chunks;
        this.citationService = citationService;
    }

    @Override
    public List<RetrievalCandidate> search(SearchQuery query) {
        String[] queryTerms = query.query().toLowerCase().split("\\s+");
        return chunks.findAll(
                        DocumentChunkSpecifications.from(query.filter())
                                .and(DocumentChunkSpecifications.textContains(query.query())),
                        PageRequest.of(0, Math.max(query.page() + 1, 1) * Math.max(query.size(), 10), Sort.by(Sort.Direction.ASC, "chunkIndex")))
                .getContent()
                .stream()
                .map(SearchMapper::toModel)
                .map(chunk -> toCandidate(chunk, queryTerms))
                .toList();
    }

    private RetrievalCandidate toCandidate(DocumentChunk chunk, String[] queryTerms) {
        String lowerText = chunk.text().toLowerCase();
        int termHits = 0;
        for (String term : queryTerms) {
            if (term.length() > 2 &&
                (lowerText.contains(term)
                 || lowerText.contains(term.replace("ä","a").replace("ö","o").replace("ü","u").replace("ß","ss")))) {
                termHits++;
            }
        }
        double tf = queryTerms.length > 0 ? (double) termHits / queryTerms.length : 0.1;
        double lengthNorm = Math.max(0.3, Math.min(1.0, 500.0 / Math.max(chunk.text().length(), 100)));
        double score = Math.max(0.05, Math.min(1.0, tf * 0.7 + lengthNorm * 0.3));
        return new RetrievalCandidate(
                new ChunkReference(chunk.id(), chunk.documentId(), chunk.documentVersion(), chunk.metadata().title(), chunk.position(), chunk.metadata().documentType()),
                chunk.text(),
                score,
                0.0,
                score,
                score,
                "keyword",
                citationService.citationFor(chunk));
    }
}
