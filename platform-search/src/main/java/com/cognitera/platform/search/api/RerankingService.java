package com.cognitera.platform.search.api;

import com.cognitera.platform.search.model.RetrievalCandidate;
import com.cognitera.platform.search.model.SearchQuery;

import java.util.List;

/** Service for reranking retrieval candidates based on query intent and scoring heuristics. */
public interface RerankingService {
    /** Reranks the given candidates for the specified query. */
    List<RetrievalCandidate> rerank(SearchQuery query, List<RetrievalCandidate> candidates);
}
