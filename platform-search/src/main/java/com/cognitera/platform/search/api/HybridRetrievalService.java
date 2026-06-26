package com.cognitera.platform.search.api;

import com.cognitera.platform.search.model.RetrievalCandidate;
import com.cognitera.platform.search.model.SearchQuery;

import java.util.List;

/** Service combining keyword and vector retrieval results into a merged, reranked candidate list. */
public interface HybridRetrievalService {
    /** Retrieves and merges keyword and vector search results for the given query. */
    List<RetrievalCandidate> retrieve(SearchQuery query);
}
