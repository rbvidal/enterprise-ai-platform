package com.cognitera.platform.ai.application;

import com.cognitera.platform.ai.api.PromptBuilder;
import com.cognitera.platform.ai.model.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Builds a grounded answer prompt by assembling system instructions, objectives, timeline, hierarchy, dossier, and authorities.
 */
@Component
public class DefaultPromptBuilder implements PromptBuilder {

    private static final String TEMPLATE_VERSION = "grounded-answer-v5";

    @Override
    public String build(PromptContext context) {
        var sources = context.retrievalContext().sources();
        var authorities = context.retrievalContext().authorityReferences();
        var objectives = context.objectives();
        var hierarchy = context.findingHierarchy();
        var dossier = context.sourceDossier();

        var factual = sources.stream()
                .filter(s -> s.sourceType() != SourceCitation.SourceType.AUTHORITATIVE)
                .toList();

        var factualPrimary = factual.stream().filter(s -> s.tier() == SourceCitation.SourceTier.PRIMARY).toList();
        var factualSupporting = factual.stream().filter(s -> s.tier() == SourceCitation.SourceTier.SUPPORTING).toList();
        var factualBackground = factual.stream().filter(s -> s.tier() == SourceCitation.SourceTier.BACKGROUND).toList();

        var primaryAuthorities = authorities.stream()
                .filter(a -> a.tier() == AuthorityReference.ReferenceTier.PRIMARY).toList();
        var supportingAuthorities = authorities.stream()
                .filter(a -> a.tier() == AuthorityReference.ReferenceTier.SUPPORTING).toList();
        var backgroundAuthorities = authorities.stream()
                .filter(a -> a.tier() == AuthorityReference.ReferenceTier.BACKGROUND).toList();

        StringBuilder prompt = new StringBuilder();
        prompt.append(context.systemInstruction()).append("\n\n");

        // USER OBJECTIVE
        if (!objectives.isEmpty()) {
            prompt.append("=== USER'S OBJECTIVE ===\n");
            prompt.append("The user's PRIMARY practical objective is:\n");
            var primary = objectives.getFirst();
            prompt.append("  ").append(primary.keyActions().getFirst()).append("\n");
            if (objectives.size() > 1) {
                prompt.append("Secondary objectives:\n");
                objectives.stream().skip(1).limit(3).forEach(o ->
                        prompt.append("  - ").append(o.keyActions().getFirst()).append("\n"));
            }
            prompt.append("\nFOCUS YOUR ANSWER ON THE PRIMARY OBJECTIVE. ");
            prompt.append("Deprioritize peripheral items that do not enable these objectives.\n\n");
        }

        // PROCEDURAL TIMELINE
        var timeline = context.retrievalContext().timeline();
        if (timeline != null && !timeline.events().isEmpty()) {
            prompt.append("=== COMMUNICATION-FIRST PROCEDURAL TIMELINE ===\n");
            prompt.append("THIS is the primary evidence. Reason from THESE documents, not from summaries.\n");
            prompt.append("Communications: ").append(timeline.communicationCount())
                    .append(" | Summaries: ").append(timeline.summaryCount()).append("\n\n");
            int idx = 1;
            for (var event : timeline.events()) {
                if (!event.isCommunication()) continue;
                SourceCitation match = findSource(sources, event.sourceDoc());
                prompt.append("[").append(idx++).append("] *** ").append(event.type())
                        .append(": ").append(event.description()).append(" ***\n");
                if (match != null && match.excerpt() != null) {
                    prompt.append(match.excerpt()).append("\n");
                }
                prompt.append("\n");
            }
            if (timeline.summaryCount() > 0) {
                prompt.append("--- Background summaries (use ONLY for context) ---\n");
                for (var event : timeline.events()) {
                    if (event.isCommunication()) continue;
                    SourceCitation match = findSource(sources, event.sourceDoc());
                    prompt.append("[").append(idx++).append("] ").append(event.type())
                            .append(": ").append(event.description()).append("\n");
                    if (match != null && match.excerpt() != null) {
                        String shortExcerpt = match.excerpt().length() > 300
                                ? match.excerpt().substring(0, 300) + "..."
                                : match.excerpt();
                        prompt.append(shortExcerpt).append("\n");
                    }
                }
                prompt.append("\n");
            }
            if (!timeline.missingEventTypes().isEmpty()) {
                prompt.append("MISSING procedural events: ")
                        .append(String.join(", ", timeline.missingEventTypes()))
                        .append(" — state this explicitly, do NOT fabricate.\n\n");
            }
        }

        // FINDING HIERARCHY
        if (hierarchy != null && !hierarchy.primaryFindings().isEmpty()) {
            prompt.append("=== FINDING HIERARCHY ===\n");
            prompt.append("IMPORTANT: Primary findings exist independently from supporting elements.\n");
            if (!hierarchy.primaryFindings().isEmpty()) {
                prompt.append("PRIMARY FINDINGS (core — main items):\n");
                for (var e : hierarchy.primaryFindings()) {
                    prompt.append("  ").append(e.label()).append(" (").append(e.description()).append(")\n");
                }
            }
            if (!hierarchy.secondaryFindings().isEmpty()) {
                prompt.append("SUPPORTING FINDINGS (does NOT replace primary findings):\n");
                for (var e : hierarchy.secondaryFindings()) {
                    prompt.append("  ").append(e.label()).append(" (").append(e.description()).append(")\n");
                }
            }
            if (!hierarchy.relationships().isEmpty()) {
                prompt.append("RELATIONSHIPS: ");
                prompt.append(String.join("; ", hierarchy.relationships())).append("\n");
            }
            prompt.append("\n");
        }

        // SOURCE DOSSIER
        if (dossier != null) {
            prompt.append("=== SOURCE DOSSIER ===\n");
            prompt.append("Coverage: ").append(String.format(Locale.US, "%.0f%%", dossier.coverageScore() * 100)).append("\n");
            prompt.append("Present roles: ").append(String.join(", ", dossier.presentRoles())).append("\n");
            if (!dossier.missingRoles().isEmpty()) {
                prompt.append("MISSING: ").append(String.join(", ", dossier.missingRoles())).append("\n");
                prompt.append("Acknowledge missing source types in your answer.\n");
            }
            prompt.append("Assessment: ").append(dossier.completenessAssessment()).append("\n\n");
        }

        // Fallback: if no timeline, include sources directly
        boolean hasTimeline = timeline != null && !timeline.events().isEmpty();
        if (!hasTimeline && !factual.isEmpty()) {
            if (!factualPrimary.isEmpty()) {
                prompt.append("=== FACTUAL SOURCES — PRIMARY ===\n");
                appendSources(prompt, factualPrimary);
            }
            if (!factualSupporting.isEmpty()) {
                prompt.append("=== FACTUAL SOURCES — SUPPORTING ===\n");
                appendSources(prompt, factualSupporting);
            }
        }

        prompt.append("QUESTION:\n").append(context.userQuestion()).append("\n\n");

        // AUTHORITY REFERENCES
        if (!authorities.isEmpty()) {
            prompt.append("=== AUTHORITY REFERENCES ===\n");
            prompt.append("These references have been ranked by relevance to the user's practical objective.\n");
            prompt.append("Apply the PRIMARY provisions to determine what remedies exist.\n\n");

            if (!primaryAuthorities.isEmpty()) {
                prompt.append("PRIMARY PROVISIONS (enable remedies/actions):\n");
                appendAuthorities(prompt, primaryAuthorities);
            }
            if (!supportingAuthorities.isEmpty()) {
                prompt.append("SUPPORTING PROVISIONS (supporting context):\n");
                appendAuthorities(prompt, supportingAuthorities);
            }
            if (!backgroundAuthorities.isEmpty()) {
                prompt.append("BACKGROUND (use only if essential):\n");
                appendAuthorities(prompt, backgroundAuthorities);
            }
            prompt.append("\n");
        }

        boolean isAuthoritativeOnly = context.retrievalScope() == RetrievalScope.AUTHORITATIVE_ONLY;

        prompt.append("IMPORTANT INSTRUCTIONS:\n");
        if (!hasTimeline || isAuthoritativeOnly) {
            prompt.append("- Base your analysis on the AUTHORITY REFERENCES provided below.\n");
        } else {
            prompt.append("- Reason from the PROCEDURAL TIMELINE above. It IS the factual evidence.\n");
            prompt.append("- Communication documents (marked ***) are PRIMARY. Summaries are background only.\n");
        }
        prompt.append("- ANSWER THE USER'S PRIMARY OBJECTIVE FIRST AND FOREMOST.\n");
        if (!authorities.isEmpty()) {
            prompt.append("- Use PRIMARY provisions to identify obligations and remedies.\n");
        }

        if (isAuthoritativeOnly) {
            prompt.append("- This is a reference analysis question. Analyze based on the references, not case facts.\n");
            prompt.append("- Address EACH sub-question the user asks explicitly.\n");
            prompt.append("- Discuss: what the references provide, what rights exist, what exceptions apply.\n");
            prompt.append("- Do NOT impose a pre-set structure. Adapt to the domain of the question.\n");
        } else {
            prompt.append("- FOREGROUND: continuing obligations, not offsets.\n");
            prompt.append("- Security instruments do NOT extinguish or replace primary obligations.\n");
            prompt.append("- DO NOT present offset as the primary or automatic remedy.\n");
            prompt.append("- Structure: (1) Primary obligation, (2) Breach, (3) Security function, (4) Steps, (5) Risks.\n");
        }

        prompt.append("- If events are missing from the timeline, state this explicitly — do NOT fabricate.\n");
        prompt.append("- Cite specific references.\n");
        prompt.append("- End with: This is not professional advice.\n");
        return prompt.toString();
    }

    private void appendAuthorities(StringBuilder sb, List<AuthorityReference> authorities) {
        int index = 1;
        for (AuthorityReference a : authorities) {
            sb.append("[").append(index++).append("] ");
            sb.append(a.referenceId());
            if (a.entryTitle() != null && !a.entryTitle().isEmpty()) {
                sb.append(" — ").append(a.entryTitle());
            }
            sb.append(" (").append(a.basis()).append(")");
            sb.append(" relevance=").append(String.format(Locale.US, "%.2f", a.relevanceScore()));
            sb.append("\n");
            if (a.excerpt() != null && !a.excerpt().isEmpty()) {
                String shortened = a.excerpt().length() > 600 ? a.excerpt().substring(0, 600) + "..." : a.excerpt();
                sb.append(shortened).append("\n");
            }
            sb.append("\n");
        }
    }

    private void appendSources(StringBuilder sb, List<SourceCitation> items) {
        int index = 1;
        for (SourceCitation s : items) {
            sb.append("[").append(index++).append("] ");
            sb.append("title=").append(s.title());
            sb.append(" type=").append(s.sourceType().name());
            sb.append(" score=").append(String.format(Locale.US, "%.3f", s.confidenceScore()));
            if (s.pageNumber() != null) sb.append(" page=").append(s.pageNumber());
            sb.append("\n").append(s.excerpt()).append("\n\n");
        }
    }

    private SourceCitation findSource(List<SourceCitation> sources, String title) {
        if (title == null) return null;
        return sources.stream()
                .filter(s -> title.equals(s.title()))
                .findFirst().orElse(null);
    }

    @Override
    public String templateVersion() {
        return TEMPLATE_VERSION;
    }
}
