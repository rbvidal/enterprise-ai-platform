package com.cognitera.platform.search.api;

import com.cognitera.platform.search.model.RetrievalCandidate;
import com.cognitera.platform.search.model.SearchQuery;

import java.util.List;

/** Provider interface for cross-encoder or LLM-based reranking of retrieval candidates. */
public interface RerankingProvider {
    /** Reranks the given candidates for the specified query. */
    List<RetrievalCandidate> rerank(SearchQuery query, List<RetrievalCandidate> candidates);
}
