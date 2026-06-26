package com.cognitera.platform.search.api;

import com.cognitera.platform.search.model.DocumentChunk;
import com.cognitera.platform.search.model.RetrievalCandidate;
import com.cognitera.platform.search.model.SearchQuery;

import java.util.List;
import java.util.UUID;

/** Provider for vector/semantic search with indexing and deletion capabilities. */
public interface VectorSearchProvider {
    /** Searches by vector similarity for the given query. */
    List<RetrievalCandidate> search(SearchQuery query);

    /** Indexes a single chunk with its embedding vector. */
    void index(DocumentChunk chunk, float[] embedding);

    /** Deletes all vectors associated with a document. */
    void deleteByDocument(UUID documentId);

    /** Indexes a batch of chunks with their corresponding embeddings. */
    default void indexBatch(List<DocumentChunk> chunks, List<float[]> embeddings) {
        if (chunks.size() != embeddings.size()) {
            throw new IllegalArgumentException("Chunks and embeddings must have same size");
        }
        for (int i = 0; i < chunks.size(); i++) {
            index(chunks.get(i), embeddings.get(i));
        }
    }
}
