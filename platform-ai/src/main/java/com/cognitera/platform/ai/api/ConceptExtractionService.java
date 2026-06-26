package com.cognitera.platform.ai.api;

import com.cognitera.platform.ai.model.ExtractedConcept;

import java.util.List;

/**
 * Extracts and classifies concepts from a query string.
 */
public interface ConceptExtractionService {
    /**
     * Classifies a query into a list of extracted concepts with confidence scores.
     */
    List<ExtractedConcept> classify(String query);
}
