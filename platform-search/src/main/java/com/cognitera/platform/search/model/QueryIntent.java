package com.cognitera.platform.search.model;

import com.cognitera.platform.document.model.DocumentType;

import java.util.Map;

/** Classified query intent with document type weights for scoring. */
public record QueryIntent(
        String intent,
        Map<DocumentType, Double> documentTypeWeights
) {
    public double weightFor(DocumentType type) {
        if (type == null) return 1.0;
        return documentTypeWeights.getOrDefault(type, 1.0);
    }
}
