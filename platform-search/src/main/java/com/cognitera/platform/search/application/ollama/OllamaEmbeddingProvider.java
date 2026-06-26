package com.cognitera.platform.search.application.ollama;

import com.cognitera.platform.search.api.EmbeddingProvider;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Ollama-based {@link EmbeddingProvider} that calls {@code /api/embeddings} to generate vector embeddings. */
public class OllamaEmbeddingProvider implements EmbeddingProvider {

    private final RestClient restClient;
    private final String model;
    private final int dimension;

    public OllamaEmbeddingProvider(OllamaEmbeddingConfig config) {
        this.model = config.model();
        this.dimension = config.dimension();
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.restClient = RestClient.builder()
                .baseUrl(config.baseUrl())
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .build();
    }

    @Override
    public float[] embed(String text) {
        Map<String, Object> request = Map.of("model", model, "prompt", text);
        OllamaEmbeddingResponse response = restClient.post()
                .uri("/api/embeddings")
                .body(request)
                .retrieve()
                .body(OllamaEmbeddingResponse.class);
        if (response == null || response.embedding() == null || response.embedding().length == 0) {
            throw new IllegalStateException("Ollama returned empty embedding");
        }
        return response.embedding();
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        return texts.stream().map(this::embed).toList();
    }

    @Override
    public int dimension() {
        return dimension;
    }

    private record OllamaEmbeddingResponse(float[] embedding) {
    }
}
