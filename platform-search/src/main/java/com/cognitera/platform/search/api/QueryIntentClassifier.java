package com.cognitera.platform.search.api;

import com.cognitera.platform.search.model.QueryIntent;

/** Classifies a query string to determine intent, document type weights, and statute boosting. */
public interface QueryIntentClassifier {
    /** Classifies the given query text and returns a {@link QueryIntent}. */
    QueryIntent classify(String query);
}
