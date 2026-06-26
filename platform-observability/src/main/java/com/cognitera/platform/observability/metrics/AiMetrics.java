package com.cognitera.platform.observability.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Central registry for AI-specific Micrometer metrics.
 * Captures inference, embedding, retrieval, and enrichment telemetry.
 */
@Component
public class AiMetrics {

    private final MeterRegistry registry;
    private final Map<String, AtomicInteger> providerGauges = new ConcurrentHashMap<>();

    public AiMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** Records a chat completion call. */
    public Timer.Sample startInference() {
        return Timer.start(registry);
    }

    /** Stops the inference timer and records provider + model tags. */
    public void stopInference(Timer.Sample sample, String provider, String model) {
        sample.stop(Timer.builder("ai.inference.duration")
                .description("LLM inference duration")
                .tag("provider", provider)
                .tag("model", model)
                .publishPercentileHistogram()
                .register(registry));
    }

    /** Records an embedding operation. */
    public void recordEmbedding(String provider, String model, int batchSize, long durationMs) {
        Timer.builder("ai.embedding.duration")
                .tag("provider", provider)
                .tag("model", model)
                .register(registry)
                .record(durationMs, TimeUnit.MILLISECONDS);
        Counter.builder("ai.embedding.count")
                .tag("provider", provider)
                .register(registry)
                .increment(batchSize);
    }

    /** Records a retrieval operation. */
    public void recordRetrieval(String mode, int resultCount, long durationMs) {
        Timer.builder("ai.retrieval.duration")
                .tag("mode", mode)
                .register(registry)
                .record(durationMs, TimeUnit.MILLISECONDS);
        registry.summary("ai.retrieval.result_count").record(resultCount);
    }

    /** Records a graph retrieval operation. */
    public void recordGraphRetrieval(int nodeCount, long durationMs) {
        Timer.builder("ai.graph.retrieval.duration")
                .register(registry)
                .record(durationMs, TimeUnit.MILLISECONDS);
        registry.summary("ai.graph.node_count").record(nodeCount);
    }

    /** Records a semantic enrichment operation. */
    public void recordEnrichment(String documentId, int entityCount, int conceptCount,
                                  int relationshipCount, long durationMs) {
        Timer.builder("ai.enrichment.duration")
                .register(registry)
                .record(durationMs, TimeUnit.MILLISECONDS);
        Counter.builder("ai.enrichment.entities").register(registry).increment(entityCount);
        Counter.builder("ai.enrichment.concepts").register(registry).increment(conceptCount);
        Counter.builder("ai.enrichment.relationships").register(registry).increment(relationshipCount);
    }

    /** Records a document ingestion operation. */
    public void recordIngestion(String documentType, int chunkCount, long durationMs) {
        Timer.builder("ai.ingestion.duration")
                .tag("document_type", documentType)
                .register(registry)
                .record(durationMs, TimeUnit.MILLISECONDS);
        Counter.builder("ai.ingestion.chunks")
                .tag("document_type", documentType)
                .register(registry)
                .increment(chunkCount);
    }

    /** Records a prompt execution with token estimates. */
    public void recordPrompt(String templateId, int estimatedTokens, long durationMs) {
        Timer.builder("ai.prompt.duration")
                .tag("template", templateId)
                .register(registry)
                .record(durationMs, TimeUnit.MILLISECONDS);
        registry.summary("ai.prompt.tokens").record(estimatedTokens);
    }

    /** Records provider availability status (0=down, 1=up). */
    public void recordProviderAvailability(String provider, boolean available) {
        AtomicInteger gauge = providerGauges.computeIfAbsent(provider, k -> {
            AtomicInteger state = new AtomicInteger(0);
            Gauge.builder("ai.provider.available", state, AtomicInteger::doubleValue)
                    .tag("provider", provider)
                    .register(registry);
            return state;
        });
        gauge.set(available ? 1 : 0);
    }

    /** Records an evaluation score. */
    public void recordEvaluation(String metric, double score) {
        registry.summary("ai.evaluation." + metric).record(score);
    }
}
