package com.cognitera.platform.ai.application;

import com.cognitera.platform.ai.api.SourceOrchestrationService;
import com.cognitera.platform.ai.model.SourceCitation;
import com.cognitera.platform.ai.model.SourceDossier;
import com.cognitera.platform.ai.model.SourceRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Builds a source dossier by classifying source citations into roles using keyword matching.
 */
@Service
public class DefaultSourceOrchestrationService implements SourceOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(DefaultSourceOrchestrationService.class);

    private static final Map<SourceRole, Set<String>> ROLE_KEYWORDS;

    static {
        ROLE_KEYWORDS = new HashMap<>();
        ROLE_KEYWORDS.put(SourceRole.ESTABLISHING_DOCUMENT, Set.of(
                "contract", "lease", "agreement", "sublease", "document"));
        ROLE_KEYWORDS.put(SourceRole.TERMINATION_NOTICE, Set.of(
                "termination", "notice", "terminated", "end",
                "dissolution", "contract end"));
        ROLE_KEYWORDS.put(SourceRole.FINANCIAL_RECORD, Set.of(
                "not paid", "did not pay", "unpaid", "default",
                "outstanding", "owing", "payment default",
                "invoice", "bill", "receipt"));
        ROLE_KEYWORDS.put(SourceRole.WARNING, Set.of(
                "warning", "final notice", "ultimatum", "consequences",
                "legal action", "deadline"));
        ROLE_KEYWORDS.put(SourceRole.ESCALATION, Set.of(
                "escalation", "attorney", "lawyer", "claim",
                "court", "legal threat", "enforcement"));
        ROLE_KEYWORDS.put(SourceRole.ACKNOWLEDGMENT, Set.of(
                "acknowledgment", "confirm", "confirmed", "admitted",
                "agreed", "accepted"));
        ROLE_KEYWORDS.put(SourceRole.ADMISSION, Set.of(
                "admitted", "admission", "conceded", "acknowledged"));
        ROLE_KEYWORDS.put(SourceRole.DAMAGES_SUPPORT, Set.of(
                "damage", "costs", "loss", "amount",
                "calculation", "invoice", "bill"));
        ROLE_KEYWORDS.put(SourceRole.CHRONOLOGY, Set.of(
                "date", "period", "timeline", "chronology",
                "sequence", "month", "year"));
        ROLE_KEYWORDS.put(SourceRole.CORRESPONDENCE, Set.of(
                "email", "letter", "correspondence",
                "communication", "reply", "response", "sent"));
    }

    private static final List<SourceRole> CORE_ROLES = List.of(
            SourceRole.ESTABLISHING_DOCUMENT,
            SourceRole.TERMINATION_NOTICE,
            SourceRole.FINANCIAL_RECORD,
            SourceRole.CORRESPONDENCE
    );

    @Override
    public SourceDossier buildDossier(List<SourceCitation> sources, String query) {
        Map<SourceRole, List<String>> byRole = new EnumMap<>(SourceRole.class);
        for (SourceRole role : SourceRole.values()) {
            byRole.put(role, new ArrayList<>());
        }

        for (SourceCitation s : sources) {
            SourceRole role = classifySource(s);
            byRole.get(role).add(s.title());
        }

        List<String> presentRoles = new ArrayList<>();
        List<String> missingRoles = new ArrayList<>();
        int coveredCoreRoles = 0;

        for (SourceRole core : CORE_ROLES) {
            if (!byRole.get(core).isEmpty()) {
                presentRoles.add(core.name());
                coveredCoreRoles++;
            } else {
                missingRoles.add(core.name());
            }
        }

        for (var entry : byRole.entrySet()) {
            if (!entry.getValue().isEmpty() && !presentRoles.contains(entry.getKey().name())) {
                presentRoles.add(entry.getKey().name());
            }
        }

        double coverageScore = CORE_ROLES.isEmpty() ? 1.0 : (double) coveredCoreRoles / CORE_ROLES.size();
        String assessment;
        if (coverageScore >= 0.75) {
            assessment = "Good source coverage — core documents present";
        } else if (coverageScore >= 0.5) {
            assessment = "Partial coverage — " + String.join(", ", missingRoles) + " missing";
        } else {
            assessment = "Weak dossier — most core evidence types missing: " + String.join(", ", missingRoles);
        }

        log.debug("Source dossier: {}/{} core roles covered, coverage={:.2f}",
                coveredCoreRoles, CORE_ROLES.size(), coverageScore);

        return new SourceDossier(byRole, presentRoles, missingRoles, coverageScore, assessment);
    }

    private SourceRole classifySource(SourceCitation source) {
        String text = (source.title() + " " + (source.excerpt() != null ? source.excerpt() : "")).toLowerCase();

        SourceRole bestRole = SourceRole.UNCLASSIFIED;
        int bestScore = 0;

        for (var entry : ROLE_KEYWORDS.entrySet()) {
            int score = 0;
            for (String keyword : entry.getValue()) {
                if (text.contains(keyword.toLowerCase())) {
                    score++;
                }
            }
            if (score > bestScore) {
                bestScore = score;
                bestRole = entry.getKey();
            }
        }

        return bestScore > 0 ? bestRole : SourceRole.UNCLASSIFIED;
    }
}
