package com.cognitera.platform.ai.model;

import java.util.List;
import java.util.Map;

/**
 * A fully reasoned answer with answer text, source citations, authority references, a finding hierarchy,
 * a source dossier, and a confidence profile.
 */
public record ReasonedAnswer(
        String answer,
        List<SourceCitation> sourceCitations,
        List<AuthorityReference> authorityReferences,
        FindingHierarchy findingHierarchy,
        SourceDossier sourceDossier,
        ConfidenceProfile confidence,
        boolean grounded
) {
    public ReasonedAnswer {
        sourceCitations = sourceCitations == null ? List.of() : List.copyOf(sourceCitations);
        authorityReferences = authorityReferences == null ? List.of() : List.copyOf(authorityReferences);
        findingHierarchy = findingHierarchy == null
                ? new FindingHierarchy(List.of(), List.of(), List.of(), List.of(), List.of())
                : findingHierarchy;
        sourceDossier = sourceDossier == null
                ? new SourceDossier(Map.of(), List.of(), List.of(), 0.0, "No assessment")
                : sourceDossier;
        confidence = confidence == null ? ConfidenceProfile.none() : confidence;
    }

    public ReasonedAnswer(String answer, List<SourceCitation> sourceCitations,
                          List<AuthorityReference> authorityReferences, double confidenceScore, boolean grounded) {
        this(answer, sourceCitations, authorityReferences, null, null,
                new ConfidenceProfile(confidenceScore, confidenceScore, confidenceScore,
                        confidenceScore, confidenceScore, ""),
                grounded);
    }

    public ReasonedAnswer(String answer, List<SourceCitation> sourceCitations,
                          double confidenceScore, boolean grounded) {
        this(answer, sourceCitations, List.of(), confidenceScore, grounded);
    }
}
