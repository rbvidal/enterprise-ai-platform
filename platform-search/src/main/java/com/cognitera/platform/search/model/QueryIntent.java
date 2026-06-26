package com.cognitera.platform.search.model;

import com.cognitera.platform.document.model.DocumentType;

import java.util.Map;

/** Classified query intent with document type weights and statute boost factor. */
public record QueryIntent(
        String intent,
        Map<DocumentType, Double> documentTypeWeights,
        boolean isLegalNormativeQuery,
        double statuteBoostFactor
) {
    public QueryIntent(String intent, Map<DocumentType, Double> documentTypeWeights) {
        this(intent, documentTypeWeights, false, 1.0);
    }

    public double weightFor(DocumentType type) {
        if (type == null) return 1.0;
        return documentTypeWeights.getOrDefault(type, 1.0);
    }
}
