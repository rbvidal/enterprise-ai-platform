package com.cognitera.platform.ai.provider;

import com.cognitera.platform.ai.api.ChatCompletionProvider;
import com.cognitera.platform.ai.config.AiProviderProperties;
import com.cognitera.platform.ai.model.ModelCapabilities;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Chat completion provider backed by Ollama's HTTP API. */
@Component
@ConditionalOnProperty(name = "platform.ai.ollama.base-url")
public class OllamaChatProvider implements ChatCompletionProvider {

    private static final Logger log = LoggerFactory.getLogger(OllamaChatProvider.class);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final AiProviderProperties properties;

    public OllamaChatProvider(ObjectMapper objectMapper, AiProviderProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public String providerName() {
        return "ollama";
    }

    @Override
    public boolean isAvailable() {
        try {
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getOllama().getBaseUrl()))
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .build();
            var response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() == 200;
        } catch (Exception e) {
            log.debug("Ollama availability check failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String complete(String prompt, ModelCapabilities capabilities) {
        String model = capabilities.model();
        if (model == null || model.isBlank()) {
            return "No model configured.";
        }
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("model", model);
            root.put("stream", false);
            ArrayNode messages = root.putArray("messages");
            ObjectNode message = messages.addObject();
            message.put("role", "user");
            message.put("content", prompt);

            var request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getOllama().getBaseUrl() + "/api/chat"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(root)))
                    .timeout(parseTimeout(properties.getOllama().getRequestTimeout()))
                    .build();

            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Ollama returned HTTP {}", response.statusCode());
                return "Provider error (HTTP " + response.statusCode() + ").";
            }

            JsonNode node = objectMapper.readTree(response.body());
            JsonNode content = node.path("message").path("content");
            if (content.isMissingNode() || !content.isTextual()) {
                return "Provider returned unexpected response format.";
            }
            return content.asText();
        } catch (Exception e) {
            log.error("Ollama request failed", e);
            return "Provider request failed: " + e.getMessage();
        }
    }

    private static Duration parseTimeout(String s) {
        if (s == null || s.isBlank()) return Duration.ofSeconds(120);
        s = s.strip().toLowerCase();
        try {
            if (s.endsWith("ms")) return Duration.ofMillis(Long.parseLong(s.substring(0, s.length() - 2)));
            if (s.endsWith("s")) return Duration.ofSeconds(Long.parseLong(s.substring(0, s.length() - 1)));
            if (s.endsWith("m")) return Duration.ofMinutes(Long.parseLong(s.substring(0, s.length() - 1)));
            return Duration.ofSeconds(Long.parseLong(s));
        } catch (NumberFormatException e) {
            return Duration.ofSeconds(120);
        }
    }
}
