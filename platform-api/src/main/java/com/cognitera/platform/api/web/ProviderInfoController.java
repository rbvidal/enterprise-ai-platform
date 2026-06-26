package com.cognitera.platform.api.web;

import com.cognitera.platform.search.api.EmbeddingProvider;
import com.cognitera.platform.search.api.VectorSearchProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller exposing status information about configured search infrastructure providers.
 */
@RestController
@RequestMapping("/api/providers")
public class ProviderInfoController {

    private static final Logger log = LoggerFactory.getLogger(ProviderInfoController.class);

    private final ObjectProvider<EmbeddingProvider> embeddingProvider;
    private final ObjectProvider<VectorSearchProvider> vectorSearchProvider;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    /**
     * Constructs the controller with optional embedding and vector search providers.
     */
    public ProviderInfoController(
            ObjectProvider<EmbeddingProvider> embeddingProvider,
            ObjectProvider<VectorSearchProvider> vectorSearchProvider) {
        this.embeddingProvider = embeddingProvider;
        this.vectorSearchProvider = vectorSearchProvider;
    }

    private String resolveOllamaBaseUrl() {
        String url = System.getProperty("platform.ai.ollama.base-url");
        if (url != null && !url.isBlank()) return url;
        url = System.getenv("OLLAMA_BASE_URL");
        if (url != null && !url.isBlank()) return url;
        url = System.getProperty("platform.search.embedding.ollama.base-url");
        if (url != null && !url.isBlank()) return url;
        return "http://localhost:11434";
    }

    /**
     * Returns the availability status of the embedding and vector search providers.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        Map<String, Object> status = new LinkedHashMap<>();
        EmbeddingProvider embedder = embeddingProvider.getIfAvailable();
        VectorSearchProvider vectors = vectorSearchProvider.getIfAvailable();

        status.put("embeddingProvider", embedder != null ? Map.of(
                "available", true,
                "type", embedder.getClass().getSimpleName(),
                "dimension", embedder.dimension()
        ) : Map.of("available", false));

        status.put("vectorSearchProvider", vectors != null ? Map.of(
                "available", true,
                "type", vectors.getClass().getSimpleName(),
                "backend", vectors instanceof com.cognitera.platform.search.application.qdrant.QdrantVectorSearchProvider
                        ? "qdrant" : "postgresql"
        ) : Map.of("available", false));

        status.put("semanticSearchReady", embedder != null && vectors != null
                && !(vectors.getClass().getSimpleName().equals("NoOpVectorSearchProvider")));

        return ResponseEntity.ok(status);
    }

    /**
     * Fetches the list of available Ollama models from the configured base URL.
     */
    @GetMapping("/models")
    public ResponseEntity<Map<String, Object>> availableModels() {
        Map<String, Object> result = new LinkedHashMap<>();
        String baseUrl = resolveOllamaBaseUrl();
        log.info("Fetching Ollama models from {}", baseUrl);
        try {
            RestClient client = RestClient.builder()
                    .baseUrl(baseUrl)
                    .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                    .build();
            @SuppressWarnings("unchecked")
            Map<String, Object> response = client.get()
                    .uri("/api/tags")
                    .retrieve()
                    .body(Map.class);
            log.debug("Ollama /api/tags response: {}", response);
            if (response != null && response.get("models") instanceof List<?> models) {
                result.put("models", models.stream()
                        .filter(m -> m instanceof Map)
                        .map(m -> (Map<?, ?>) m)
                        .filter(m -> m.get("name") instanceof String)
                        .map(m -> Map.of("name", (String) m.get("name")))
                        .toList());
                log.info("Found {} Ollama model(s)", result.get("models"));
            } else {
                result.put("models", List.of());
                log.warn("Ollama /api/tags returned unexpected format: {}", response);
            }
            result.put("available", true);
            result.put("baseUrl", baseUrl);
        } catch (Exception e) {
            log.warn("Failed to fetch Ollama models from {}: {}", baseUrl, e.getMessage());
            result.put("models", List.of());
            result.put("available", false);
            result.put("error", e.getMessage());
            result.put("baseUrl", baseUrl);
        }
        return ResponseEntity.ok(result);
    }
}
