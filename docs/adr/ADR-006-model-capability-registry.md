# ADR-006 — Model Capability Registry

## Status

Accepted. Implemented in `platform-ai/src/main/java/com/cognitera/platform/ai/application/DefaultModelCapabilityRegistry.java`.

## Context

Different AI models have different capabilities (streaming, vision, JSON mode, tool calling, embeddings). Business logic should not hardcode assumptions like "model X supports JSON." Instead, capabilities should be queryable through a central registry, enabling the `ProviderRouter` to make intelligent selection decisions.

## Decision

Implement a **Model Capability Registry** (`ModelCapabilityRegistry` interface) that describes every available model's capabilities:

| Capability | Examples |
|-----------|----------|
| `supportsStreaming` | qwen2.5:7b (true), nomic-embed-text (false) |
| `supportsVision` | gpt-4o (true) |
| `supportsJson` / `supportsToolCalling` | gpt-4o, llama3.2 (true) |
| `supportsEmbeddings` | nomic-embed-text (true) |
| `supportsStructuredOutput` | gpt-4o (true) |
| `maxContextWindow` | 128K (gpt-4o), 32K (qwen2.5) |
| `maxOutputTokens` | 16384 (gpt-4o) |
| `estimatedLatencyMs` | 500 (qwen2.5 local), 1200 (gpt-4o cloud) |
| `estimatedCostPer1kTokens` | $5.00 (gpt-4o), $0.00 (local Ollama) |
| `preferredUseCases` | "rag", "analysis", "summarization" |

The registry supports querying by:
- Exact model name: `get("gpt-4o")`
- Provider: `findByProvider("ollama")`
- Capability: `findByCapability(EMBEDDING)`
- Provider + capability: `findByProviderAndCapability("ollama", STREAMING)`

The `DefaultModelCapabilityRegistry` seeds with 6 known models (4 Ollama, 2 OpenAI). The `ProviderRouter` uses the registry for capability-based routing decisions.

## Alternatives Considered

- **Hardcoded if/else chains**: Rejected. Unmaintainable with growing model lists.
- **Runtime model discovery via API**: Deferred. Provider APIs (Ollama `/api/tags`, OpenAI `/models`) could populate the registry dynamically, but add latency and failure modes.
- **No registry — trust the provider**: Rejected. The platform needs to know capabilities before calling a model (e.g., "does this model support JSON mode?").

## Consequences

- **Capability-aware routing**: `ProviderRouter` selects models based on required capabilities
- **Static seed data**: Registry is populated at startup with known models; new models require code changes
- **Query interface**: Rich query API enables future use cases (e.g., "find the cheapest model that supports vision")

## Trade-offs

- Static data can become stale as providers add new models
- Estimated costs and latencies are approximations, not real-time measurements
- No runtime validation that the model actually supports claimed capabilities

## Future Evolution

- Dynamic discovery from provider `/models` endpoints
- Runtime capability verification (send test prompts to verify claims)
- User-provided model registrations via configuration
- Integration with provider cost APIs for real-time pricing

See also: [[ADR-003]], [[ADR-004]], [[ADR-005]]
