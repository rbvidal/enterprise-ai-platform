package com.cognitera.platform.ai.api;

import com.cognitera.platform.ai.model.SourceCitation;
import com.cognitera.platform.ai.model.SourceDossier;
import com.cognitera.platform.ai.model.SourceRole;

import java.util.List;
import java.util.Set;

/**
 * Enforces provenance rules, ensuring primary sources are not displaced by derived ones.
 */
public interface ProvenanceService {

    /**
     * The result of a provenance check, separating primary and derived sources.
     */
    record ProvenanceCheckResult(
            List<SourceCitation> primaryOnly,
            List<SourceCitation> derived,
            SourceDossier primaryDossier,
            Set<SourceRole> rolesFilledByDerived,
            String assessment
    ) {}

    /**
     * Enforces provenance by separating primary sources from derived sources and flagging violations.
     */
    ProvenanceCheckResult enforce(List<SourceCitation> sources, SourceDossier dossier);
}
