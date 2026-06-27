# ADR-011 — Retrieval Orchestration with Intent-Based Strategy Selection

## Status

Accepted. Implemented in `platform-ai/src/main/java/com/cognitera/platform/ai/application/DefaultRetrievalOrchestrator.java`.

## Context

Different query types benefit from different retrieval strategies. A factual lookup ("What was the revenue in Q2?") benefits from keyword search. An exploratory question ("What capabilities does the platform have?") benefits from hybrid search. An index inspection query should not waste resources on vector search.

## Decision

Implement a **Retrieval Orchestrator** that selects the retrieval strategy based on classified query intent:

```
User Query → Intent Classification → Strategy Selection → Search Execution → Result
```

Strategy mapping (deterministic and explainable):

| QueryIntent | SearchMode | Rationale |
|-------------|-----------|-----------|
| `INDEX_INSPECTION`, `CORPUS_DISCOVERY` | `KEYWORD` | Exact match queries; vector search unnecessary |
| `QUESTION_ANSWERING`, `WORKSPACE_ANALYSIS`, `SOURCE_ANALYSIS` | `HYBRID` | Complex queries benefit from keyword + vector fusion |
| `DOCUMENT_RESEARCH`, `DOCUMENT_LOOKUP` | `SEMANTIC` | Conceptual queries benefit from vector similarity |

The orchestrator produces `RetrievalOrchestrationResult` with full **explainability metadata**: intent, strategy, mode, prompt template, result counts, fusion method, reranking status, timing, and a step-by-step `traceLog`.

## Alternatives Considered

- **Always run all retrievers**: Rejected. Wastes resources on inappropriate strategies (e.g., vector search for exact ID lookups).
- **User-specified strategy**: Rejected. Users should not need to understand retrieval internals.
- **ML-based strategy selection**: Deferred. A trained classifier could outperform keyword-based intent classification, but keyword rules are deterministic, explainable, and sufficient for initial release.

## Consequences

- **Deterministic**: Same query always produces same strategy
- **Explainable**: Every retrieval decision is recorded in `traceLog`
- **Efficient**: Only appropriate retrievers are executed
- **Prompt-aware**: Retrieves the latest prompt template from `PromptRegistry`

## Trade-offs

- Keyword-based intent classification can misclassify queries
- Strategy mapping is static (no runtime adaptation based on result quality)
- Graph retrieval is not automatically selected by the orchestrator (requires explicit `HYBRID_GRAPH` mode)

## Future Evolution

- ML-based intent classification for higher accuracy
- Feedback loop: if results are poor, retry with expanded strategy
- Graph strategy auto-selection when enriched entities are detected in the query

See also: [[ADR-005]], [[ADR-008]], [[ADR-012]]
