package com.cognitera.platform.ai.application;

import com.cognitera.platform.ai.api.ProvenanceService;
import com.cognitera.platform.ai.model.SourceCitation;
import com.cognitera.platform.ai.model.SourceDossier;
import com.cognitera.platform.ai.model.SourceRole;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Enforces provenance rules by distinguishing primary and derived sources and flagging role violations.
 */
@Service
public class DefaultProvenanceService implements ProvenanceService {

    private static final Set<String> DERIVED_TITLE_PATTERNS = Set.of(
            "summary", "overview", "analysis",
            "chronology", "assessment", "opinion",
            "evaluation", "report"
    );

    private static final Set<String> PRIMARY_TITLE_PATTERNS = Set.of(
            "contract", "termination", "notice",
            "reminder", "warning",
            "email", "letter",
            "invoice", "receipt",
            "confirmation", "acknowledgment",
            "payment", "transfer"
    );

    private static final Set<SourceRole> COMMUNICATION_ROLES = Set.of(
            SourceRole.WARNING,
            SourceRole.ESCALATION,
            SourceRole.ACKNOWLEDGMENT,
            SourceRole.CORRESPONDENCE,
            SourceRole.FINANCIAL_RECORD
    );

    @Override
    public ProvenanceCheckResult enforce(List<SourceCitation> sources, SourceDossier dossier) {
        List<SourceCitation> primary = new ArrayList<>();
        List<SourceCitation> derived = new ArrayList<>();
        Set<SourceRole> rolesFilledByDerived = new LinkedHashSet<>();

        for (SourceCitation s : sources) {
            boolean isDerived = isDerivedSource(s);

            if (isDerived) {
                derived.add(s);
                for (SourceRole role : COMMUNICATION_ROLES) {
                    if (dossier.sourcesByRole().getOrDefault(role, List.of()).contains(s.title())) {
                        rolesFilledByDerived.add(role);
                    }
                }
            } else {
                primary.add(s);
            }
        }

        SourceDossier primaryDossier = buildDossier(primary);

        StringBuilder assessment = new StringBuilder();
        assessment.append("Primary: ").append(primary.size()).append(", Derived: ").append(derived.size());
        if (!rolesFilledByDerived.isEmpty()) {
            assessment.append(". WARNING: Derived sources falsely satisfied these communication roles: ");
            assessment.append(String.join(", ", rolesFilledByDerived.stream().map(Enum::name).toList()));
            assessment.append(". These MUST be backed by primary original documents. ");
            assessment.append("Targeted retrieval should re-fetch primary sources for these roles.");
        } else if (!derived.isEmpty()) {
            assessment.append(". Derived sources present but did not displace primary communication evidence.");
        }

        return new ProvenanceCheckResult(primary, derived, primaryDossier, rolesFilledByDerived, assessment.toString());
    }

    private boolean isDerivedSource(SourceCitation source) {
        String title = source.title() != null ? source.title().toLowerCase() : "";
        String excerpt = source.excerpt() != null ? source.excerpt().toLowerCase() : "";

        for (String pattern : DERIVED_TITLE_PATTERNS) {
            if (title.contains(pattern) || excerpt.length() > 0 && excerpt.contains(pattern)) {
                for (String primaryPattern : PRIMARY_TITLE_PATTERNS) {
                    if (title.contains(primaryPattern)) {
                        return false;
                    }
                }
                return true;
            }
        }

        for (String pattern : PRIMARY_TITLE_PATTERNS) {
            if (title.contains(pattern)) {
                return false;
            }
        }

        return false;
    }

    private SourceDossier buildDossier(List<SourceCitation> sources) {
        Map<SourceRole, List<String>> byRole = new EnumMap<>(SourceRole.class);
        for (SourceRole role : SourceRole.values()) byRole.put(role, new ArrayList<>());

        Map<SourceRole, Set<String>> roleKeywords = Map.of(
                SourceRole.ESTABLISHING_DOCUMENT, Set.of("contract", "lease", "agreement"),
                SourceRole.TERMINATION_NOTICE, Set.of("termination", "notice"),
                SourceRole.FINANCIAL_RECORD, Set.of("not paid", "did not pay", "unpaid", "invoice"),
                SourceRole.WARNING, Set.of("warning", "final notice"),
                SourceRole.ESCALATION, Set.of("escalation", "attorney", "claim"),
                SourceRole.ACKNOWLEDGMENT, Set.of("acknowledgment", "confirm"),
                SourceRole.CORRESPONDENCE, Set.of("email", "letter"),
                SourceRole.DAMAGES_SUPPORT, Set.of("damage", "costs", "amount"),
                SourceRole.CHRONOLOGY, Set.of("date", "period", "timeline")
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
        Set<SourceRole> mandatory = Set.of(SourceRole.ESTABLISHING_DOCUMENT,
                SourceRole.TERMINATION_NOTICE, SourceRole.FINANCIAL_RECORD, SourceRole.WARNING);
        int covered = 0;
        for (SourceRole role : mandatory) {
            if (!byRole.get(role).isEmpty()) { present.add(role.name()); covered++; }
            else { missing.add(role.name()); }
        }
        double coverage = mandatory.isEmpty() ? 1.0 : (double) covered / mandatory.size();
        return new SourceDossier(byRole, present, missing, coverage,
                coverage >= 0.75 ? "Good (primary only)" : coverage >= 0.5 ? "Partial — " + String.join(", ", missing) + " missing" : "Weak");
    }
}
