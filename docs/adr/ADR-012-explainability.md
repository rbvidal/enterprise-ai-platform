# ADR-012 — Explainability by Default

## Status

Accepted. Implemented in `platform-ai/src/main/java/com/cognitera/platform/ai/model/RetrievalOrchestrationResult.java` and `platform-ai/src/main/java/com/cognitera/platform/ai/model/InferenceMetadata.java`.

## Context

AI systems must be auditable. When the platform generates an answer, stakeholders need to know: which model generated it, which prompt template was used, which documents were retrieved, which retrieval strategy was selected, and how long each step took. Without explainability, AI output is a black box.

## Decision

Make **explainability a first-class concern** on every inference. Every AI response carries `InferenceMetadata` and every retrieval carries `RetrievalOrchestrationResult` with:

| Metadata | Example |
|----------|---------|
| `provider` | "ollama" |
| `model` | "qwen2.5:14b" |
| `promptTemplateId` | "rag-answer/v1" |
| `retrievalStrategy` | "HYBRID" |
| `keywordResultCount` | 12 |
| `vectorResultCount` | 8 |
| `graphNodeCount` | 3 |
| `totalSourceCount` | 15 |
| `fusionMethod` | "weighted-linear-fusion" |
| `rerankingApplied` | true |
| `rerankingProvider` | "ollama-cross-encoder" |
| `retrievalStartedAt` / `retrievalCompletedAt` | timestamps |
| `traceLog` | ["Orchestration started", "Intent: QUESTION_ANSWERING", ...] |
| `evaluationScores` | {grounding: 0.72, faithfulness: 0.85} |

The `explain()` method produces a human-readable summary:
```
Intent: QUESTION_ANSWERING | Strategy: HYBRID | Prompt: rag-answer/v1 | Sources: 15 | Fusion: weighted-linear-fusion | Reranking: yes (ollama-cross-encoder) | Duration: 245ms
```

Explainability is **built into the orchestration layer**, not bolted on as an afterthought. Business services don't call explainability APIs — the orchestrator populates metadata automatically.

## Alternatives Considered

- **Optional explainability**: Rejected. Explainability should never be optional in an enterprise AI platform.
- **Separate explainability service**: Rejected. Would require duplicating orchestration state. Building metadata into the orchestration result ensures consistency.
- **User-facing explainability only**: Rejected. Internal diagnostics are as important as user-facing explanations.

## Consequences

- **Every inference is auditable**: Full traceability from query to answer
- **Deterministic**: Same query → same strategy → same explainability output
- **Low overhead**: Metadata is collected during normal execution, not as a separate pass
- **Future-proof**: New retrieval sources automatically appear in metadata

## Trade-offs

- `traceLog` is append-only string list (not structured log entries)
- `RetrievalOrchestrationResult` uses a builder with 19 setters (verbose but explicit)
- Metadata is not yet exposed through a dedicated API endpoint

## Future Evolution

- Structured trace events with typed metadata (not string list)
- Explainability REST API (`GET /api/inferences/{id}/explain`)
- Visualization of retrieval decisions (Sankey diagram of query → strategy → results)
- Differential explainability: "why was document A ranked above document B?"

See also: [[ADR-011]], [[ADR-013]], [[ADR-014]]
