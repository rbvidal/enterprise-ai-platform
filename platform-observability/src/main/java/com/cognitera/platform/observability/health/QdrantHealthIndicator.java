package com.cognitera.platform.observability.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Health indicator that checks the Qdrant vector database. */
@Component
@ConditionalOnProperty(name = "platform.search.qdrant.host")
public class QdrantHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(QdrantHealthIndicator.class);

    private final String qdrantUrl;

    public QdrantHealthIndicator() {
        String host = env("QDRANT_HOST", "localhost");
        String port = env("QDRANT_REST_PORT", "6333");
        this.qdrantUrl = "http://" + host + ":" + port;
    }

    @Override
    public Health health() {
        try {
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(qdrantUrl + "/collections"))
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .build();
            var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() == 200) {
                return Health.up().withDetail("url", qdrantUrl).build();
            }
            return Health.down().withDetail("url", qdrantUrl)
                    .withDetail("status", response.statusCode()).build();
        } catch (Exception e) {
            log.debug("Qdrant health check failed: {}", e.getMessage());
            return Health.down().withDetail("url", qdrantUrl)
                    .withDetail("error", e.getMessage()).build();
        }
    }

    private static String env(String key, String fallback) {
        String val = System.getenv(key);
        return val != null && !val.isBlank() ? val : fallback;
    }
}
