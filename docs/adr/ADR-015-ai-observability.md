# ADR-015 — AI Observability with Micrometer

## Status

Accepted. Implemented in `platform-observability/src/main/java/com/cognitera/platform/observability/metrics/AiMetrics.java`.

## Context

AI operations are complex, multi-step processes with multiple external dependencies. Without observability, diagnosing failures (is the LLM slow? is Qdrant down? is enrichment producing too few entities?) requires log diving. Production AI systems require metrics.

## Decision

Implement **AI-specific observability** using Micrometer + Prometheus, integrated into a dedicated `platform-observability` module:

| Metric | Type | Tags |
|--------|------|------|
| `ai.inference.duration` | Timer (percentile histogram) | provider, model |
| `ai.embedding.duration` | Timer | provider, model |
| `ai.retrieval.duration` | Timer | mode (keyword/semantic/hybrid) |
| `ai.graph.retrieval.duration` | Timer | — |
| `ai.enrichment.duration` | Timer | — |
| `ai.ingestion.duration` | Timer | document_type |
| `ai.prompt.duration` | Timer | template |
| `ai.provider.available` | Gauge (0/1) | provider |
| `ai.embedding.count` | Counter | provider |
| `ai.enrichment.entities` | Counter | — |
| `ai.evaluation.*` | Summary | metric name |

Metrics are exposed via `/actuator/prometheus` and `/actuator/health`. Health indicators check Ollama, Qdrant, and provider availability.

The `platform-observability` module is reusable across future applications. It depends only on Micrometer and Spring Boot Actuator — no platform-specific dependencies.

## Alternatives Considered

- **Logging only**: Rejected. Logs are not aggregatable or queryable at scale. Metrics enable dashboards and alerting.
- **OpenTelemetry from day one**: Deferred. Micrometer provides a simpler API that can export to OTLP when OpenTelemetry is adopted.
- **Metrics in each module**: Rejected. Centralizing metrics in `platform-observability` ensures consistent naming, avoids duplication, and enables reuse.

## Consequences

- **Operational visibility**: Every AI operation produces metrics
- **Reusable module**: Future applications get observability by depending on `platform-observability`
- **Standard format**: Prometheus exposition format enables Grafana dashboards
- **Health endpoints**: Liveness, readiness, and dependency health checks via Actuator

## Trade-offs

- Metrics are defined but not yet fully wired into all services (`AiMetrics` is injected in fewer places than ideal)
- No custom Grafana dashboard definitions
- No alerting rules (would be added in deployment configuration, not in the platform)

## Future Evolution

- Wire `AiMetrics` into all AI services (`AiService`, `SearchService`, `DefaultEnrichmentService`)
- OpenTelemetry exporter for distributed tracing
- Pre-built Grafana dashboard JSON
- Alert definitions for critical metrics (LLM latency > threshold, provider down)

See also: [[ADR-002]], [[ADR-016]]
