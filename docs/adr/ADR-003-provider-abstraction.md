# ADR-003 — Provider Abstraction via Service Provider Interface (SPI)

## Status

Accepted. Implemented in `platform-ai/src/main/java/com/cognitera/platform/ai/api/ChatCompletionProvider.java` and related interfaces.

## Context

AI backends (Ollama, OpenAI, Anthropic, Gemini) expose different APIs, authentication schemes, and request/response formats. Business logic must not depend on any specific provider implementation. Adding a new provider should require zero changes to orchestration code.

## Decision

Define **SPI interfaces** for every AI capability. Implementations are conditionally activated based on configuration:

| Interface | Implementations | Activation |
|-----------|----------------|------------|
| `ChatCompletionProvider` | `OllamaChatProvider`, `OpenAiChatProvider` | `@ConditionalOnProperty` |
| `EmbeddingProvider` | `OllamaEmbeddingProvider` | `@ConditionalOnProperty` |
| `RerankingProvider` | `OllamaRerankingProvider` | `@ConditionalOnProperty` |
| `VectorSearchProvider` | `QdrantVectorSearchProvider`, `NoOpVectorSearchProvider` | Conditional on host |
| `GraphSearchProvider` | `Neo4jGraphSearchAdapter`, `NoOpGraphSearchProvider` | Conditional on Neo4j |
| `EvaluationService` | `DefaultEvaluationService` | Always active |

The `ProviderRouter` (`DefaultProviderRouter`) selects a provider using a 4-tier strategy:
1. Model name prefix (`openai:gpt-4o` → openai)
2. Capability match (streaming → capable provider)
3. Preferred provider
4. First available fallback

Business services depend on interfaces, never on concrete implementations.

## Alternatives Considered

- **Hardcoded provider selection in business logic**: Rejected. Would couple orchestration to specific providers and make adding new providers expensive.
- **Spring AI abstraction**: Rejected. At implementation time, Spring AI 1.0.0 provided limited control over provider selection and lacked the capability registry concept. The dependency was removed during build audit.
- **Factory pattern without SPI**: Rejected. An SPI with conditional beans is more idiomatic in Spring and supports auto-discovery.

## Consequences

- **Provider-agnostic business logic**: `AiService` depends on `ChatCompletionProvider`, not Ollama or OpenAI
- **Conditional activation**: Providers activate only when their configuration is present
- **No-code provider addition**: Implement `ChatCompletionProvider` + `@Component` + `@ConditionalOnProperty`
- **Graceful degradation**: When no provider is available, `ProviderRouter` throws a clear exception

## Trade-offs

- Each provider implements its own HTTP client (no shared HTTP infrastructure)
- Provider implementations must handle their own error translation
- Capability discovery is static (seeded in `DefaultModelCapabilityRegistry`) rather than dynamic

## Future Evolution

- Dynamic capability discovery via provider API introspection
- Shared HTTP client infrastructure with configurable retry/timeout
- Provider cost and latency metadata for intelligent routing

See also: [[ADR-004]], [[ADR-006]], [[ADR-016]]
