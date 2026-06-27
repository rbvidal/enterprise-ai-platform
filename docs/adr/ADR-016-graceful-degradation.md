# ADR-016 — Graceful Degradation Over Hard Failures

## Status

Accepted. Implemented throughout the platform via `@ConditionalOnProperty`, `@ConditionalOnBean`, `ObjectProvider`, and `NoOp*` fallback beans.

## Context

The Enterprise AI Platform depends on multiple external services: Ollama (LLM), Qdrant (vector DB), Neo4j (graph DB), PostgreSQL (relational DB). In production, any of these may be unavailable. The platform must continue functioning with reduced capabilities rather than failing entirely.

## Decision

Design every external dependency as **optional**. The platform uses multiple Spring mechanisms for graceful degradation:

| Mechanism | Example | Behavior |
|-----------|---------|----------|
| `@ConditionalOnProperty` | `OllamaChatProvider` requires `platform.ai.ollama.base-url` | Bean not created if config absent |
| `@ConditionalOnBean` | `Neo4jGraphSearchAdapter` requires `GraphEnrichmentService` | Bean not created if Neo4j unavailable |
| `ObjectProvider<T>` | `DefaultDocumentIngestionProcessor` injects `ObjectProvider<EnrichmentHook>` | Null-safe getIfAvailable() |
| `NoOp*` fallbacks | `NoOpVectorSearchProvider`, `NoOpGraphSearchProvider` | Return empty results, never throw |
| `try-catch` wrappers | `Neo4jGraphSearchAdapter.search()` | Log warning, return empty list |
| `isAvailable()` checks | `GraphSearchProvider.isAvailable()`, `ChatCompletionProvider.isAvailable()` | Caller checks before invoking |
| `ProviderRouter` fallback | 4-tier selection skips unavailable providers | Throws only when ALL unavailable |

The degradation is **visible**: health indicators report status, metrics track failures, logs record the reason. The platform never silently degrades.

## Alternatives Considered

- **Mandatory all services**: Rejected. Would prevent development without full infrastructure.
- **Circuit breaker only**: Rejected. Circuit breakers (Resilience4j) are valuable for transient failures but don't address the "service never configured" case. Conditional beans handle both.
- **Feature flags**: Rejected. Adds complexity. Conditional bean activation is simpler and more idiomatic in Spring.

## Consequences

- **Platform starts with zero infrastructure** (except PostgreSQL for basic operation)
- **157 tests pass without any external services** (H2 in-memory, no Ollama/Qdrant/Neo4j)
- **Clear failure modes**: When a service is down, the reason is logged and surfaced in health endpoints
- **Gradual capability ramp**: Start with keyword search → add Qdrant for vector → add Neo4j for GraphRAG → add Ollama for LLM

## Trade-offs

- Some operations silently return empty results (graph search) rather than surfacing errors to users
- No retry logic for transient failures (deferred to Resilience4j integration)
- Health indicators currently read env vars directly rather than Spring config (known drift, documented)

## Future Evolution

- Resilience4j integration for retry, circuit breaker, rate limiter
- Health indicators refactored to inject `@ConfigurationProperties`
- Degradation event publishing to audit log
- User-facing capability indicators ("vector search unavailable")

See also: [[ADR-003]], [[ADR-008]], [[ADR-009]], [[ADR-010]], [[ADR-015]]
