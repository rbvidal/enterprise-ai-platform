# ADR-019 — Multi-Layer Testing Strategy

## Status

Accepted. Implemented across `platform-ai/src/test/` and `platform-api/src/test/`. Documented in `TESTING.md`.

## Context

An Enterprise AI Platform requires confidence that every architectural subsystem behaves correctly. Tests must validate behavior, not implementation details. A reader should understand the platform architecture by reading the tests.

## Decision

Adopt a **5-layer testing strategy**:

| Layer | Location | Execution | Purpose |
|-------|----------|-----------|---------|
| **Unit** | `platform-ai/src/test/.../unit/` | `mvn test` | Validate components in isolation |
| **Integration** | `platform-api/src/test/.../ai/` | `mvn verify` | Validate subsystems with Spring context |
| **Architecture** | `platform-api/src/test/.../architecture/` | `mvn verify` | Validate end-to-end behavior |
| **Contract** | `platform-ai/src/test/.../contract/` | `mvn verify` | Validate SPI stability |
| **Playwright (UI)** | `e2e-tests/playwright/` | `mvn verify -Pui-tests` | Validate browser behavior |

Key principles:
- **Behavior over implementation**: Tests verify what the platform does, not how it does it
- **Executable documentation**: Test names describe behavior (`"routes openai:gpt-4o to openai provider"`)
- **Realistic scenarios**: Architecture tests use a `test-corpus/` with real document types and expected behaviors
- **Contract tests**: Every `ChatCompletionProvider` implementation must satisfy the abstract `ChatCompletionProviderContract`
- **No mocks for platform internals**: Unit tests mock external providers; integration tests use real Spring context with H2

157 tests validate: Prompt Registry (12), Model Capability Registry (12), Provider Router (14), Provider Contracts (5), Retrieval Orchestrator (3), Evaluation Engine (8), Workflow Engine (10), Semantic Enrichment (14), Graceful Degradation (9), GraphRAG (4), AI Pipeline (10), Browser UI (12), Platform (91).

## Alternatives Considered

- **Coverage-driven testing**: Rejected. Coverage percentage does not measure architectural confidence. 157 behavioral tests provide more value than 500 getter tests.
- **BDD framework (Cucumber)**: Rejected. Adds framework complexity. JUnit 5 `@DisplayName` + `@Nested` provides sufficient behavior description.
- **No UI tests**: Rejected. Browser tests validate user-visible behavior that unit tests cannot.

## Consequences

- **Architecture as tests**: Test structure mirrors platform architecture
- **Confidence, not coverage**: Every major subsystem has executable proof of correctness
- **CI-ready**: `mvn clean verify` runs all tests except Playwright (separate profile)
- **Contract protection**: New providers must satisfy SPI contracts

## Trade-offs

- Playwright tests require a running application (separate Maven profile)
- `test-corpus/` is small (3 documents) — sufficient for architectural validation, not exhaustive
- No performance regression tests yet

## Future Evolution

- Performance smoke test suite (`mvn verify -Pperformance`)
- Extended test corpus with multilingual documents
- Snapshot tests for prompt rendering output
- Automated evaluation regression: "did this prompt change degrade faithfulness?"

See also: [[ADR-020]], [TESTING.md](../../TESTING.md)
