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

/** Chat completion provider for OpenAI-compatible APIs (OpenAI, vLLM, LiteLLM, etc.). */
@Component
@ConditionalOnProperty(name = "platform.ai.openai.api-key")
public class OpenAiChatProvider implements ChatCompletionProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAiChatProvider.class);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final AiProviderProperties properties;

    public OpenAiChatProvider(ObjectMapper objectMapper, AiProviderProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public String providerName() {
        return "openai";
    }

    @Override
    public boolean isAvailable() {
        return properties.getOpenai().getApiKey() != null && !properties.getOpenai().getApiKey().isBlank();
    }

    @Override
    public String complete(String prompt, ModelCapabilities capabilities) {
        String model = capabilities.model();
        if (model == null || model.isBlank()) {
            model = properties.getOpenai().getChatModel();
        }
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("model", model);
            root.put("max_tokens", capabilities.contextWindowTokens());
            ArrayNode messages = root.putArray("messages");
            ObjectNode message = messages.addObject();
            message.put("role", "user");
            message.put("content", prompt);

            var request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getOpenai().getBaseUrl() + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + properties.getOpenai().getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(root)))
                    .timeout(Duration.ofSeconds(60))
                    .build();

            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("OpenAI returned HTTP {}: {}", response.statusCode(), response.body());
                return "Provider error (HTTP " + response.statusCode() + ").";
            }

            JsonNode node = objectMapper.readTree(response.body());
            JsonNode choices = node.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                JsonNode content = choices.get(0).path("message").path("content");
                if (content.isTextual()) return content.asText();
            }
            return "Provider returned unexpected response format.";
        } catch (Exception e) {
            log.error("OpenAI request failed", e);
            return "Provider request failed: " + e.getMessage();
        }
    }
}
