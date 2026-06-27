# ADR-004 — Provider Router for Intelligent Model Selection

## Status

Accepted. Implemented in `platform-ai/src/main/java/com/cognitera/platform/ai/application/DefaultProviderRouter.java`.

## Context

With multiple AI providers available (Ollama local, OpenAI cloud), the platform must select the appropriate provider for each inference request. Selection criteria include model name, required capabilities, user preference, and provider availability.

## Decision

Implement a **Provider Router** (`ProviderRouter` interface) with a deterministic 4-tier selection strategy:

1. **Model prefix routing**: `openai:gpt-4o` routes to the OpenAI provider, `ollama:qwen2.5` routes to Ollama
2. **Capability-based selection**: If a specific capability is requested (e.g., `STREAMING`), the router selects a provider whose models support that capability, as defined in the `ModelCapabilityRegistry`
3. **Preferred provider**: If the caller specifies a preferred provider, it is selected if available
4. **First available fallback**: The first provider reporting `isAvailable() == true` is selected

If no provider is available, the router throws `IllegalStateException` with a clear message.

Business services call `router.routeChat(InferenceRequest)` — they request capabilities, not specific providers.

## Alternatives Considered

- **Direct injection of a single provider**: Rejected. Would make multi-provider setups impossible and require code changes to switch providers.
- **Random/round-robin selection**: Rejected. Non-deterministic behavior is harder to debug and audit.
- **Cost/latency-aware routing**: Deferred to future evolution. The router architecture supports adding routing dimensions.

## Consequences

- **Deterministic routing**: Same input always produces same routing decision
- **Auditable**: Every routing decision is logged (`log.debug("Routed '{}' to provider '{}' by model prefix", ...)`)
- **Extensible**: Adding a routing dimension means adding a strategy tier, not rewriting the router
- **Clear failure mode**: Unavailable providers are skipped with explicit fallthrough

## Trade-offs

- Static capability registry requires manual updates when new models are added
- No runtime performance tracking for latency/cost-based routing
- Model prefix convention (`provider:model`) is a convention, not enforced by types

## Future Evolution

- Cost-aware routing using `ModelCapability.estimatedCostPer1kTokens()`
- Latency-aware routing using `ModelCapability.estimatedLatencyMs()`
- Region-aware routing for multi-region deployments
- Dynamic capability discovery from provider APIs

See also: [[ADR-003]], [[ADR-005]], [[ADR-006]]
