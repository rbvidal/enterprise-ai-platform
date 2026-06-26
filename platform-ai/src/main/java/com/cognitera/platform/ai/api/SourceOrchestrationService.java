package com.cognitera.platform.ai.api;

import com.cognitera.platform.ai.model.SourceCitation;
import com.cognitera.platform.ai.model.SourceDossier;

import java.util.List;

/**
 * Orchestrates the classification of source citations into a source dossier.
 */
public interface SourceOrchestrationService {
    /**
     * Builds a {@link SourceDossier} classifying sources by role and computing coverage.
     */
    SourceDossier buildDossier(List<SourceCitation> sources, String query);
}
