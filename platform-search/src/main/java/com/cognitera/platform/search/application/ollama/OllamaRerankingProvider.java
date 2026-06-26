package com.cognitera.platform.search.application.ollama;

import com.cognitera.platform.search.api.RerankingProvider;
import com.cognitera.platform.search.model.RetrievalCandidate;
import com.cognitera.platform.search.model.SearchQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Ollama-based cross-encoder {@link RerankingProvider} using a chat model to score relevance of candidate excerpts. */
public class OllamaRerankingProvider implements RerankingProvider {

    private static final Logger log = LoggerFactory.getLogger(OllamaRerankingProvider.class);

    private final RestClient restClient;
    private final String model;

    public OllamaRerankingProvider(String baseUrl, String model) {
        this.model = model;
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(60)).build();
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .build();
    }

    @Override
    public List<RetrievalCandidate> rerank(SearchQuery query, List<RetrievalCandidate> candidates) {
        if (candidates.size() <= 1) {
            return candidates;
        }
        int topN = Math.min(candidates.size(), 15);
        List<RetrievalCandidate> toRerank = candidates.subList(0, topN);
        List<RetrievalCandidate> rest = candidates.subList(topN, candidates.size());

        try {
            String prompt = buildRerankPrompt(query.query(), toRerank);
            String response = callOllama(prompt);
            Map<Integer, Double> scores = parseScores(response, toRerank.size());

            List<RetrievalCandidate> rescored = new ArrayList<>();
            for (int i = 0; i < toRerank.size(); i++) {
                RetrievalCandidate c = toRerank.get(i);
                double rerankScore = scores.getOrDefault(i, c.rankingScore());
                double finalScore = 0.2 * c.rankingScore() + 0.8 * rerankScore;
                rescored.add(new RetrievalCandidate(
                        c.chunk(), c.text(), c.keywordScore(), c.vectorScore(),
                        finalScore,
                        c.confidenceScore(), "reranked", c.citation()));
            }
            rescored.sort(Comparator.comparingDouble(RetrievalCandidate::rankingScore).reversed());
            rescored.addAll(rest);
            return rescored;
        } catch (Exception e) {
            log.warn("Reranking failed, returning original order: {}", e.getMessage());
            return candidates;
        }
    }

    private String buildRerankPrompt(String query, List<RetrievalCandidate> candidates) {
        StringBuilder sb = new StringBuilder();
        sb.append("Rate how relevant each document excerpt is to this query.\n\n");
        sb.append("Query: ").append(query).append("\n\n");
        for (int i = 0; i < candidates.size(); i++) {
            String excerpt = candidates.get(i).text();
            if (excerpt.length() > 600) {
                excerpt = excerpt.substring(0, 600);
            }
            sb.append("[").append(i).append("] ").append(excerpt).append("\n\n");
        }
        sb.append("Respond ONLY with scores line by line, format: index=score\n");
        sb.append("Score from 0 (completely irrelevant) to 10 (highly relevant).\n");
        sb.append("Example: 0=8 1=3 2=9");
        return sb.toString();
    }

    private String callOllama(String prompt) {
        Map<String, Object> request = Map.of(
                "model", model,
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "stream", false,
                "options", Map.of("temperature", 0.0)
        );
        OllamaChatResponse response = restClient.post()
                .uri("/api/chat")
                .body(request)
                .retrieve()
                .body(OllamaChatResponse.class);
        if (response == null || response.message() == null || response.message().content() == null) {
            throw new IllegalStateException("Ollama returned empty reranking response");
        }
        return response.message().content();
    }

    private Map<Integer, Double> parseScores(String response, int count) {
        Map<Integer, Double> scores = new java.util.HashMap<>();
        for (String line : response.split("[\\n;]")) {
            line = line.trim();
            int eq = line.indexOf('=');
            if (eq < 0) continue;
            try {
                int index = Integer.parseInt(line.substring(0, eq).trim());
                double score = Double.parseDouble(line.substring(eq + 1).trim()) / 10.0;
                scores.put(index, Math.max(0.0, Math.min(1.0, score)));
            } catch (NumberFormatException ignored) {
            }
        }
        if (scores.isEmpty()) {
            String cleaned = response.replaceAll("[^0-9,.\\s=]", "").trim();
            String[] parts = cleaned.split("[,\\s]+");
            for (int i = 0; i < Math.min(parts.length, count); i++) {
                try {
                    scores.put(i, Math.max(0.0, Math.min(1.0, Double.parseDouble(parts[i]) / 10.0)));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return scores;
    }

    record OllamaChatResponse(OllamaMessage message) {}
    record OllamaMessage(String role, String content) {}
}
