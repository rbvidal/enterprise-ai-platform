package com.cognitera.platform.search.api;

import com.cognitera.platform.search.model.SearchQuery;
import com.cognitera.platform.search.model.SearchResultPage;

/** Facade for executing search queries and returning paginated results. */
public interface SearchFacade {
    /** Executes a search query and returns a paginated result page. */
    SearchResultPage search(SearchQuery query);
}
