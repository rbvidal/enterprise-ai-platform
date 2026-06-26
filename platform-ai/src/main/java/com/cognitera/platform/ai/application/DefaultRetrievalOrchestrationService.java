package com.cognitera.platform.ai.application;

import com.cognitera.platform.ai.api.ProvenanceService;
import com.cognitera.platform.ai.api.RetrievalOrchestrationService;
import com.cognitera.platform.ai.model.SourceCitation;
import com.cognitera.platform.ai.model.SourceDossier;
import com.cognitera.platform.ai.model.SourceRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Orchestrates quota-enforced retrieval to fill gaps in source roles.
 * Uses domain-agnostic role categories instead of law-specific ones.
 */
@Service
public class DefaultRetrievalOrchestrationService implements RetrievalOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(DefaultRetrievalOrchestrationService.class);

    private final ProvenanceService provenanceService;

    public DefaultRetrievalOrchestrationService(ProvenanceService provenanceService) {
        this.provenanceService = provenanceService;
    }

    private static final Set<SourceRole> MANDATORY_ROLES = new LinkedHashSet<>(List.of(
            SourceRole.ESTABLISHING_DOCUMENT,
            SourceRole.CORRESPONDENCE
    ));

    private static final Map<SourceRole, List<String>> ROLE_QUERIES = Map.of(
            SourceRole.ESTABLISHING_DOCUMENT, List.of(
                    "document report record reference", "document report file"),
            SourceRole.CORRESPONDENCE, List.of(
                    "communication correspondence message", "email letter note"),
            SourceRole.FINANCIAL_RECORD, List.of(
                    "financial invoice payment record", "budget expense cost"),
            SourceRole.CHRONOLOGY, List.of(
                    "date timeline chronology sequence", "date period schedule timeline")
    );

    @Override
    public QuotaEnforcedRetrieval retrieveWithQuotas(
            String query,
            List<SourceCitation> initialResults,
            SourceDossier initialDossier,
            SearchDelegate searchDelegate) {

        List<SourceCitation> allSources = new ArrayList<>(initialResults);
        Set<String> seenChunks = new HashSet<>();
        for (SourceCitation s : initialResults) {
            seenChunks.add(s.chunkId().toString());
        }

        var provenanceCheck = provenanceService.enforce(initialResults, initialDossier);
        if (!provenanceCheck.rolesFilledByDerived().isEmpty()) {
            log.debug("Provenance: derived sources falsely satisfied roles {}",
                    provenanceCheck.rolesFilledByDerived());
        }

        List<String> targetedRoles = new ArrayList<>();
        int targetedCount = 0;

        for (SourceRole role : MANDATORY_ROLES) {
            boolean missing = initialDossier.sourcesByRole().getOrDefault(role, List.of()).isEmpty();
            boolean falselySatisfied = provenanceCheck.rolesFilledByDerived().contains(role);

            if (missing || falselySatisfied) {
                log.debug("Source role {} missing — launching targeted retrieval", role);
                targetedRoles.add(role.name());

                List<String> queries = ROLE_QUERIES.getOrDefault(role, List.of());
                for (String targetedQuery : queries) {
                    List<SourceCitation> roleResults = searchDelegate.search(
                            query + " " + targetedQuery, 3);
                    for (SourceCitation s : roleResults) {
                        if (seenChunks.add(s.chunkId().toString())) {
                            allSources.add(s);
                            targetedCount++;
                        }
                    }
                }
            }
        }

        SourceDossier updatedDossier = buildDossier(allSources);

        log.debug("Quota enforcement: {} initial -> +{} targeted -> {} total",
                initialResults.size(), targetedCount, allSources.size());

        return new QuotaEnforcedRetrieval(
                allSources,
                updatedDossier,
                initialResults.size(),
                targetedCount,
                targetedRoles,
                targetRolesFound(targetedRoles, allSources, targetedCount));
    }

    private String targetRolesFound(List<String> roles, List<SourceCitation> sources, int count) {
        if (roles.isEmpty()) return "NO_ROLES_TARGETED";
        return "TARGETED:" + String.join(",", roles) + ":" + count;
    }

    private SourceDossier buildDossier(List<SourceCitation> sources) {
        Map<SourceRole, List<String>> byRole = new EnumMap<>(SourceRole.class);
        for (SourceRole role : SourceRole.values()) {
            byRole.put(role, new ArrayList<>());
        }
        Map<SourceRole, Set<String>> roleKeywords = Map.of(
                SourceRole.ESTABLISHING_DOCUMENT, Set.of("document", "report", "record", "file"),
                SourceRole.CORRESPONDENCE, Set.of("email", "letter", "message", "communication"),
                SourceRole.FINANCIAL_RECORD, Set.of("invoice", "payment", "financial", "budget"),
                SourceRole.CHRONOLOGY, Set.of("date", "timeline", "schedule", "period")
        );

        for (SourceCitation s : sources) {
            String text = ((s.title() != null ? s.title() : "") + " " +
                    (s.excerpt() != null ? s.excerpt() : "")).toLowerCase();
            for (var entry : roleKeywords.entrySet()) {
                for (String kw : entry.getValue()) {
                    if (text.contains(kw.toLowerCase())) {
                        byRole.get(entry.getKey()).add(s.title());
                        break;
                    }
                }
            }
        }

        List<String> present = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        int covered = 0;
        for (SourceRole role : MANDATORY_ROLES) {
            if (!byRole.get(role).isEmpty()) {
                present.add(role.name());
                covered++;
            } else {
                missing.add(role.name());
            }
        }
        double coverage = MANDATORY_ROLES.isEmpty() ? 1.0 : (double) covered / MANDATORY_ROLES.size();
        String assessment = coverage >= 0.75 ? "Good" : coverage >= 0.5 ? "Partial" : "Weak";

        return new SourceDossier(byRole, present, missing, coverage, assessment);
    }
}
