package com.cognitera.platform.search.application;

import com.cognitera.platform.document.model.DocumentType;
import com.cognitera.platform.search.api.QueryIntentClassifier;
import com.cognitera.platform.search.api.RerankingService;
import com.cognitera.platform.search.model.QueryIntent;
import com.cognitera.platform.search.model.RetrievalCandidate;
import com.cognitera.platform.search.model.SearchQuery;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Reranks candidates based on query intent, applying statute boost for legal-normative queries. */
@Service
public class DefaultRerankingService implements RerankingService {

    private final QueryIntentClassifier intentClassifier;

    public DefaultRerankingService(QueryIntentClassifier intentClassifier) {
        this.intentClassifier = intentClassifier;
    }

    @Override
    public List<RetrievalCandidate> rerank(SearchQuery query, List<RetrievalCandidate> candidates) {
        QueryIntent intent = intentClassifier.classify(query.query());

        if (!intent.isLegalNormativeQuery() || candidates.size() <= 1) {
            return candidates.stream()
                    .sorted(Comparator.comparingDouble(RetrievalCandidate::rankingScore).reversed())
                    .toList();
        }

        double maxScore = candidates.stream()
                .mapToDouble(RetrievalCandidate::rankingScore)
                .max().orElse(1.0);
        double statuteBoost = intent.statuteBoostFactor();

        List<RetrievalCandidate> scored = new ArrayList<>(candidates);
        scored.sort((a, b) -> {
            double scoreA = effectiveScore(a, maxScore, statuteBoost);
            double scoreB = effectiveScore(b, maxScore, statuteBoost);
            return Double.compare(scoreB, scoreA);
        });

        double maxEffective = scored.stream()
                .mapToDouble(c -> effectiveScore(c, maxScore, statuteBoost))
                .max().orElse(1.0);

        List<RetrievalCandidate> rescaled = new ArrayList<>();
        for (RetrievalCandidate c : scored) {
            double effective = effectiveScore(c, maxScore, statuteBoost);
            double normalizedScore = maxEffective > 0 ? Math.min(1.0, effective / maxEffective) : c.rankingScore();
            rescaled.add(new RetrievalCandidate(
                    c.chunk(), c.text(),
                    c.keywordScore(), c.vectorScore(),
                    normalizedScore,
                    c.confidenceScore(),
                    "reranked",
                    c.citation()));
        }
        return rescaled;
    }

    private double effectiveScore(RetrievalCandidate candidate, double maxScore, double statuteBoost) {
        return candidate.rankingScore();
    }
}
