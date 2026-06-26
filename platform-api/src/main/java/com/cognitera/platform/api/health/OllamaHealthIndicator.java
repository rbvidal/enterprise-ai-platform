package com.cognitera.platform.api.health;

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

/** Health indicator that checks connectivity to the Ollama server. */
@Component
@ConditionalOnProperty(name = "platform.ai.ollama.base-url")
public class OllamaHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(OllamaHealthIndicator.class);

    private final String baseUrl;
    private final HttpClient httpClient;

    public OllamaHealthIndicator() {
        this.baseUrl = env("OLLAMA_BASE_URL", "http://localhost:11434");
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    @Override
    public Health health() {
        try {
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl))
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .build();
            var response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() == 200) {
                return Health.up().withDetail("base_url", baseUrl).build();
            }
            return Health.down().withDetail("base_url", baseUrl)
                    .withDetail("status", response.statusCode()).build();
        } catch (Exception e) {
            log.debug("Ollama health check failed: {}", e.getMessage());
            return Health.down().withDetail("base_url", baseUrl)
                    .withDetail("error", e.getMessage()).build();
        }
    }

    private static String env(String key, String fallback) {
        String val = System.getenv(key);
        return val != null && !val.isBlank() ? val : fallback;
    }
}
