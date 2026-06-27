# Enterprise AI Platform — Testing Guide

## Testing Philosophy

Tests in this repository serve as **executable architecture documentation**. They prove that the platform behaves correctly rather than merely verifying component wiring.

We favor behavioral tests over implementation tests. A reader should understand the platform architecture by reading the tests.

## Test Layers

```
┌─────────────────────────────────────────┐
│              Playwright (UI)             │  mvn verify -Pui-tests
├─────────────────────────────────────────┤
│         Architecture & Workflow          │  mvn verify
├─────────────────────────────────────────┤
│           Integration Tests              │  mvn verify
├─────────────────────────────────────────┤
│             Unit Tests                   │  mvn test
└─────────────────────────────────────────┘
```

### Unit Tests — `mvn test`
- **Location**: `platform-ai/src/test/java/.../unit/`, `platform-api/src/test/java/.../`
- **Purpose**: Validate individual components in isolation
- **Characteristics**: Fast, no Spring context (where possible), mock external dependencies

### Integration Tests — `mvn verify`
- **Location**: `platform-api/src/test/java/.../ai/`, `platform-api/src/test/java/.../graph/`
- **Purpose**: Validate subsystems with real Spring context
- **Characteristics**: Spring Boot test slices, H2 database, no external services

### Architecture Tests — `mvn verify`
- **Location**: `platform-api/src/test/java/.../architecture/`
- **Purpose**: Validate that platform subsystems interact correctly
- **Characteristics**: End-to-end flows, behavioral validation

### Workflow Tests — `mvn verify`
- **Location**: `platform-ai/src/test/java/.../unit/workflow/`
- **Purpose**: Validate workflow engine state transitions
- **Characteristics**: Pure Java, no Spring context

### Playwright Tests — `mvn verify -Pui-tests`
- **Location**: `e2e-tests/playwright/`
- **Purpose**: Validate user-visible behavior in a browser
- **Characteristics**: Requires running application, Chromium browser

### Performance Smoke Tests — `mvn verify -Pperformance`
- **Location**: `platform-api/src/test/java/.../performance/`
- **Purpose**: Detect catastrophic regressions in latency or throughput
- **Characteristics**: Not benchmarking — just detecting 10x regressions

## How to Execute

```bash
# Full build + unit + integration + architecture tests
mvn clean verify

# Unit tests only
mvn test

# Browser UI tests (requires running app on port 8080)
mvn verify -Pui-tests

# Performance smoke tests
mvn verify -Pperformance

# Single test class
mvn test -pl platform-ai -Dtest=PromptRegistryTest
```

## AI Regression Dataset

Located at `test-corpus/`. Contains representative documents and expected behaviors.

```
test-corpus/
├── technical-spec.txt       # Technical specification document
├── financial-report.txt     # Financial report with entities
├── contract-sample.txt      # Contract with obligations and dates
├── questions.txt            # Representative questions
└── expected-behaviors.txt   # Expected architectural behaviors
```

The regression dataset validates:
- Entity extraction accuracy
- Concept detection
- Relationship extraction
- Retrieval strategy selection
- Graph participation
- Citation generation
- Explainability metadata

**Golden rule**: We validate architectural behavior, not exact LLM output. LLM text varies — retrieval strategy, entity types, citation structure, and metadata do not.

## Resilience Tests

The platform must demonstrate graceful degradation:

| Failure | Expected Behavior |
|---------|-------------------|
| Neo4j unavailable | Graph retrieval disabled; keyword + vector continue |
| Qdrant unavailable | Vector retrieval disabled; keyword continues |
| Ollama unavailable | Graceful error returned; platform stays up |
| Provider unavailable | Router selects fallback provider |
| Evaluation disabled | Inference succeeds without evaluation metadata |

## Contract Tests

Provider SPIs are protected by contract tests. Every implementation of:

- `ChatCompletionProvider` — must return non-null for valid prompts
- `EmbeddingProvider` — must return vectors of configured dimension
- `GraphSearchProvider` — must return empty list when unavailable
- `EvaluationService` — must produce scores in [0,1] range

New provider implementations should extend the contract test base classes.

## CI Strategy

```yaml
# .github/workflows/ci.yml (recommended)
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: 21 }
      - run: mvn clean verify

  ui-tests:
    runs-on: ubuntu-latest
    needs: test
    steps:
      - uses: actions/checkout@v4
      - run: docker compose up -d postgres qdrant
      - run: mvn spring-boot:run -pl platform-api &
      - run: mvn verify -Pui-tests
```

## Adding New Tests

1. **Unit test**: Test a single class. Mock dependencies. Fast.
2. **Integration test**: Test subsystem interaction. Use Spring test slices.
3. **Architecture test**: Test end-to-end behavior. Validate the architecture.
4. **Contract test**: Protect SPI stability. Extend base contract class.
5. **Playwright test**: Simulate real user behavior. Use Maven profile.

## Test Naming Convention

Tests describe behavior:

```
✅ "resolves latest version of rag-answer prompt"
✅ "routes openai:gpt-4o to openai provider"
✅ "skips unavailable provider and selects available one"

❌ "testPromptRegistry"
❌ "testGetLatest"
❌ "providerRouterTest1"
```
