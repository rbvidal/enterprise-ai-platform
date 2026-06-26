package com.cognitera.platform.ai.api;

import com.cognitera.platform.ai.model.AuthorityReference;
import com.cognitera.platform.ai.model.ExtractedConcept;

import java.util.List;

/**
 * Retrieves authority references matching the given concepts.
 */
public interface AuthorityRetrievalService {
    /**
     * Retrieves a list of authority references based on the query and extracted concepts.
     */
    List<AuthorityReference> retrieveAuthorities(String query, List<ExtractedConcept> concepts);
}
