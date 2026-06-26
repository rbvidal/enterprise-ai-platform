package com.cognitera.platform.search.application.qdrant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

/** Manages the Qdrant vector collection lifecycle, ensuring the configured collection exists on startup. */
public class QdrantCollectionManager {

    private static final Logger log = LoggerFactory.getLogger(QdrantCollectionManager.class);

    private final RestClient restClient;
    private final QdrantProperties properties;

    public QdrantCollectionManager(QdrantProperties properties) {
        this.properties = properties;
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .build();
    }

    /** Checks for an existing collection and creates one with the configured dimension and Cosine distance if missing. */
    public void ensureCollectionExists() {
        try {
            Map<String, Object> collectionInfo = restClient.get()
                    .uri("/collections/{collection}", properties.collection())
                    .retrieve()
                    .body(Map.class);
            if (collectionInfo != null) {
                log.info("Qdrant collection '{}' already exists", properties.collection());
                return;
            }
        } catch (Exception e) {
            log.info("Qdrant collection '{}' not found, creating...", properties.collection());
        }
        createCollection();
    }

    private void createCollection() {
        Map<String, Object> request = Map.of(
                "vectors", Map.of(
                        "size", properties.vectorDimension(),
                        "distance", "Cosine"
                )
        );
        restClient.put()
                .uri("/collections/{collection}", properties.collection())
                .body(request)
                .retrieve()
                .toBodilessEntity();
        log.info("Qdrant collection '{}' created with dimension={}", properties.collection(), properties.vectorDimension());
    }
}
