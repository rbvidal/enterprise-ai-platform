package com.cognitera.platform.ai.application;

import com.cognitera.platform.ai.api.GroundingService;
import com.cognitera.platform.ai.model.AuthorityReference;
import com.cognitera.platform.ai.model.ConfidenceProfile;
import com.cognitera.platform.ai.model.ReasonedAnswer;
import com.cognitera.platform.ai.model.RetrievalContext;
import com.cognitera.platform.ai.model.SourceCitation;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Grounds a raw AI answer against retrieved evidence by reattributing sources and computing confidence.
 */
@Service
public class DefaultGroundingService implements GroundingService {

    @Override
    public ReasonedAnswer ground(String rawAnswer, RetrievalContext retrievalContext) {
        if (retrievalContext.sources().isEmpty() && retrievalContext.authorityReferences().isEmpty()) {
            return new ReasonedAnswer("Insufficient retrieved evidence to answer the question.",
                    List.of(), List.of(), 0.0, false);
        }

        List<SourceCitation> attributed = reattribute(rawAnswer, retrievalContext.sources());

        double sourceConfidence = attributed.stream()
                .mapToDouble(SourceCitation::confidenceScore)
                .average()
                .orElse(0.0);
        double authorityConfidence = retrievalContext.authorityReferences().stream()
                .mapToDouble(a -> a.tier() == AuthorityReference.ReferenceTier.PRIMARY ? 0.9 : 0.5)
                .average()
                .orElse(0.0);
        double semanticConf = retrievalContext.findingHierarchy().primaryFindings().isEmpty() ? 0.3 : 0.8;
        double completenessConf = retrievalContext.sourceDossier().coverageScore();
        double overallConf = Math.min(1.0,
                sourceConfidence * 0.2 + authorityConfidence * 0.25 + semanticConf * 0.25 + completenessConf * 0.3);

        ConfidenceProfile confProfile = new ConfidenceProfile(
                sourceConfidence, authorityConfidence, semanticConf, completenessConf,
                overallConf,
                "Source=" + String.format("%.2f", sourceConfidence)
                        + " Semantic=" + String.format("%.2f", semanticConf)
                        + " Completeness=" + String.format("%.2f", completenessConf));

        return new ReasonedAnswer(rawAnswer, attributed, retrievalContext.authorityReferences(),
                retrievalContext.findingHierarchy(), retrievalContext.sourceDossier(),
                confProfile, true);
    }

    private List<SourceCitation> reattribute(String answer, List<SourceCitation> sources) {
        String[] answerWords = answer.toLowerCase().split("\\s+");
        Set<String> answerTokens = new HashSet<>();
        for (String w : answerWords) {
            if (w.length() > 3) answerTokens.add(w);
        }

        double maxOrigScore = sources.stream().mapToDouble(SourceCitation::confidenceScore).max().orElse(0.0);

        List<SourceCitation> rescored = new ArrayList<>();
        for (SourceCitation s : sources) {
            double overlap = computeOverlap(answerTokens, s.excerpt());
            double boost;

            if (s.sourceType() == SourceCitation.SourceType.AUTHORITATIVE) {
                boost = s.confidenceScore() * (1.0 + overlap * 1.0 + 0.15);
            } else {
                boost = s.confidenceScore() * (1.0 + overlap * 2.0);
            }
            boost = Math.min(1.0, boost);

            SourceCitation.SourceTier tier;
            if (s.sourceType() == SourceCitation.SourceType.AUTHORITATIVE) {
                if (boost >= maxOrigScore * 0.5 || boost >= 0.3) {
                    tier = SourceCitation.SourceTier.PRIMARY;
                } else if (boost >= maxOrigScore * 0.3 || boost >= 0.15) {
                    tier = SourceCitation.SourceTier.SUPPORTING;
                } else {
                    tier = SourceCitation.SourceTier.BACKGROUND;
                }
            } else {
                if (boost >= maxOrigScore * 1.1 || boost >= 0.45) {
                    tier = SourceCitation.SourceTier.PRIMARY;
                } else if (boost >= maxOrigScore * 0.5 || boost >= 0.15) {
                    tier = SourceCitation.SourceTier.SUPPORTING;
                } else {
                    tier = SourceCitation.SourceTier.BACKGROUND;
                }
            }

            rescored.add(new SourceCitation(
                    s.documentId(), s.chunkId(), s.documentVersion(), s.title(),
                    s.pageNumber(), s.startOffset(), s.endOffset(), s.excerpt(),
                    boost, tier, s.sourceType()));
        }

        rescored.sort(Comparator.comparingDouble(SourceCitation::confidenceScore).reversed());

        if (!rescored.isEmpty() && rescored.getFirst().tier() != SourceCitation.SourceTier.PRIMARY) {
            SourceCitation top = rescored.getFirst();
            rescored.set(0, new SourceCitation(
                    top.documentId(), top.chunkId(), top.documentVersion(), top.title(),
                    top.pageNumber(), top.startOffset(), top.endOffset(), top.excerpt(),
                    top.confidenceScore(), SourceCitation.SourceTier.PRIMARY, top.sourceType()));
        }

        return rescored;
    }

    private double computeOverlap(Set<String> answerTokens, String excerpt) {
        if (excerpt == null || excerpt.isBlank() || answerTokens.isEmpty()) return 0.0;
        String[] words = excerpt.toLowerCase().split("\\s+");
        int hits = 0;
        for (String w : words) {
            if (w.length() > 3 && answerTokens.contains(w)) hits++;
        }
        return Math.min(1.0, (double) hits / Math.max(answerTokens.size(), 1));
    }
}
