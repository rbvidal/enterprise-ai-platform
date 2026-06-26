package com.cognitera.platform.search.application;

import com.cognitera.platform.audit.api.AuditEventType;
import com.cognitera.platform.document.model.DocumentType;
import com.cognitera.platform.search.api.GraphSearchProvider;
import com.cognitera.platform.search.api.HybridRetrievalService;
import com.cognitera.platform.search.api.KeywordSearchProvider;
import com.cognitera.platform.search.api.QueryIntentClassifier;
import com.cognitera.platform.search.api.RerankingProvider;
import com.cognitera.platform.search.api.RerankingService;
import com.cognitera.platform.search.api.VectorSearchProvider;
import com.cognitera.platform.search.model.QueryIntent;
import com.cognitera.platform.search.model.RetrievalCandidate;
import com.cognitera.platform.search.model.SearchMode;
import com.cognitera.platform.search.model.SearchQuery;
import org.springframework.stereotype.Service;

import org.springframework.beans.factory.ObjectProvider;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Combines keyword and vector search results, merges them, and optionally applies cross-encoder reranking. */
@Service
public class DefaultHybridRetrievalService implements HybridRetrievalService {

    private final KeywordSearchProvider keywordSearchProvider;
    private final VectorSearchProvider vectorSearchProvider;
    private final GraphSearchProvider graphSearchProvider;
    private final RerankingService rerankingService;
    private final SearchAuditPublisher auditPublisher;
    private final QueryIntentClassifier intentClassifier;
    private final ObjectProvider<RerankingProvider> rerankingProvider;

    public DefaultHybridRetrievalService(
            KeywordSearchProvider keywordSearchProvider,
            VectorSearchProvider vectorSearchProvider,
            GraphSearchProvider graphSearchProvider,
            RerankingService rerankingService,
            SearchAuditPublisher auditPublisher,
            QueryIntentClassifier intentClassifier,
            ObjectProvider<RerankingProvider> rerankingProvider) {
        this.keywordSearchProvider = keywordSearchProvider;
        this.vectorSearchProvider = vectorSearchProvider;
        this.graphSearchProvider = graphSearchProvider;
        this.rerankingService = rerankingService;
        this.auditPublisher = auditPublisher;
        this.intentClassifier = intentClassifier;
        this.rerankingProvider = rerankingProvider;
    }

    @Override
    public List<RetrievalCandidate> retrieve(SearchQuery query) {
        QueryIntent intent = intentClassifier.classify(query.query());
        List<RetrievalCandidate> keywordResults = shouldRunKeyword(query.mode())
                ? keywordSearchProvider.search(query)
                : List.of();
        List<RetrievalCandidate> vectorResults = shouldRunVector(query.mode())
                ? vectorSearchProvider.search(query)
                : List.of();
        List<RetrievalCandidate> graphResults = shouldRunGraph(query.mode())
                ? graphSearchProvider.search(query)
                : List.of();

        List<RetrievalCandidate> merged = merge(keywordResults, vectorResults, graphResults, intent);
        List<RetrievalCandidate> baseReranked = rerankingService.rerank(query, merged);
        RerankingProvider crossReranker = rerankingProvider.getIfAvailable();
        List<RetrievalCandidate> crossReranked = crossReranker != null
                ? crossReranker.rerank(query, baseReranked)
                : baseReranked;

        auditPublisher.emit(
                query.context().actorId(),
                query.context().tenantId(),
                AuditEventType.RETRIEVAL_EXECUTED,
                query.context().requestId(),
                Map.of(
                        "mode", query.mode().name(),
                        "intent", intent.intent(),
                        "keywordCandidates", Integer.toString(keywordResults.size()),
                        "vectorCandidates", Integer.toString(vectorResults.size()),
                        "graphCandidates", Integer.toString(graphResults.size()),
                        "resultCount", Integer.toString(crossReranked.size())));
        return crossReranked;
    }

    private boolean shouldRunKeyword(SearchMode mode) {
        return mode == SearchMode.KEYWORD || mode == SearchMode.HYBRID || mode == SearchMode.HYBRID_GRAPH;
    }

    private boolean shouldRunVector(SearchMode mode) {
        return mode == SearchMode.SEMANTIC || mode == SearchMode.HYBRID || mode == SearchMode.HYBRID_GRAPH;
    }

    private boolean shouldRunGraph(SearchMode mode) {
        return (mode == SearchMode.GRAPH || mode == SearchMode.HYBRID_GRAPH)
                && graphSearchProvider.isAvailable();
    }

    private List<RetrievalCandidate> merge(List<RetrievalCandidate> keywordResults,
                                            List<RetrievalCandidate> vectorResults,
                                            List<RetrievalCandidate> graphResults,
                                            QueryIntent intent) {
        Map<String, RetrievalCandidate> merged = new LinkedHashMap<>();
        keywordResults.forEach(c -> merged.put(c.chunk().chunkId().toString(), c));
        vectorResults.forEach(c -> merged.merge(
                c.chunk().chunkId().toString(), c,
                (first, second) -> combine(first, second, intent)));
        graphResults.forEach(c -> merged.merge(
                c.chunk().chunkId().toString(), c,
                (existing, graph) -> combineWithGraph(existing, graph)));
        return List.copyOf(merged.values());
    }

    private RetrievalCandidate combine(RetrievalCandidate first, RetrievalCandidate second, QueryIntent intent) {
        double keywordScore = Math.max(first.keywordScore(), second.keywordScore());
        double vectorScore = Math.max(first.vectorScore(), second.vectorScore());
        double docTypeWeight = intent.weightFor(first.chunk().documentType());
        double rankingScore = ((keywordScore * 0.40) + (vectorScore * 0.40) + (Math.max(first.confidenceScore(), second.confidenceScore()) * 0.20)) * docTypeWeight;
        return new RetrievalCandidate(
                first.chunk(), first.text(),
                keywordScore, vectorScore,
                Math.min(1.0, rankingScore),
                Math.max(first.confidenceScore(), second.confidenceScore()),
                "hybrid", first.citation());
    }

    private RetrievalCandidate combineWithGraph(RetrievalCandidate existing, RetrievalCandidate graph) {
        double graphBoost = graph.rankingScore() > 0 ? graph.rankingScore() * 0.15 : 0;
        double newScore = Math.min(1.0, existing.rankingScore() + graphBoost);
        return new RetrievalCandidate(
                existing.chunk(), existing.text(),
                existing.keywordScore(), existing.vectorScore(),
                newScore, existing.confidenceScore(),
                "hybrid+graph", existing.citation());
    }
}
