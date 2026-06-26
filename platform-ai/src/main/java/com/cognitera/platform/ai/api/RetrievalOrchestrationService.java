package com.cognitera.platform.ai.api;

import com.cognitera.platform.ai.model.SourceCitation;
import com.cognitera.platform.ai.model.SourceDossier;

import java.util.List;

/**
 * Orchestrates targeted retrieval with quota enforcement to fill gaps in source coverage.
 */
public interface RetrievalOrchestrationService {

    /**
     * The result of quota-enforced retrieval, with all sources, updated dossier, and strategy metadata.
     */
    record QuotaEnforcedRetrieval(
            List<SourceCitation> sources,
            SourceDossier dossier,
            int initialResults,
            int targetedResults,
            List<String> targetedRoles,
            String strategy
    ) {}

    /**
     * Performs quota-enforced retrieval, launching targeted searches to fill missing mandatory roles.
     */
    QuotaEnforcedRetrieval retrieveWithQuotas(
            String query,
            List<SourceCitation> initialResults,
            SourceDossier initialDossier,
            SearchDelegate searchDelegate
    );

    /**
     * Delegate for performing a search, to be implemented by the search layer.
     */
    @FunctionalInterface
    interface SearchDelegate {
        List<SourceCitation> search(String query, int maxResults);
    }
}
