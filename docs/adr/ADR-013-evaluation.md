# ADR-013 — Evaluation Engine

## Status

Accepted. Implemented in `platform-ai/src/main/java/com/cognitera/platform/ai/application/DefaultEvaluationService.java`.

## Context

AI-generated answers must be evaluated for quality. Without evaluation, there is no feedback loop to detect hallucination, poor grounding, or irrelevant answers. Evaluation must run automatically as part of the inference pipeline, not as a separate manual process.

## Decision

Implement an **Evaluation Engine** that automatically evaluates every retrieval + inference cycle:

| Metric | Method | Range |
|--------|--------|-------|
| `groundingScore` | Context length vs answer length ratio | [0, 1] |
| `citationCoverage` | Citation markers [1], [2] in answer | [0, 1] |
| `faithfulness` | Inverse of hallucination indicators | [0, 1] |
| `answerRelevance` | Term overlap between question and answer | [0, 1] |
| `contextRelevance` | Term overlap between question and context | [0, 1] |
| `hallucinationIndicators` | Uncertainty phrase detection | integer ≥ 0 |
| `passed` | Composite quality gate | boolean |

The `DefaultEvaluationService` uses heuristic methods (regex patterns, term overlap, length ratios). In production, a dedicated evaluation LLM or framework (deepeval, ragas) would replace these heuristics.

Evaluation is wired into `DefaultRetrievalOrchestrator` — it runs after retrieval and before the result is returned. Every `RetrievalOrchestrationResult` carries evaluation scores in its metadata.

## Alternatives Considered

- **No evaluation**: Rejected. An AI platform without quality feedback is irresponsible engineering.
- **LLM-as-judge only**: Deferred. Requires a second LLM call, adding latency and cost. Heuristic evaluation provides fast, deterministic feedback. LLM evaluation can be added as a separate evaluation profile.
- **Human evaluation only**: Rejected. Does not scale and cannot run in CI pipelines.

## Consequences

- **Automatic quality gate**: Every retrieval is scored; low-quality retrievals are flagged
- **Deterministic**: Heuristic methods produce consistent, reproducible scores
- **Lightweight**: Evaluation adds negligible latency (no LLM calls)

## Trade-offs

- Heuristic methods are less accurate than LLM-based evaluation
- `hallucinationIndicators` uses regex patterns that miss sophisticated hallucinations
- No benchmark dataset integration for regression testing

## Future Evolution

- LLM-as-judge evaluation (separate profile, gated by configuration)
- RAG evaluation framework integration (deepeval, ragas)
- Evaluation benchmark dataset with ground truth annotations
- Automated regression testing: "does this prompt change improve or degrade evaluation scores?"

See also: [[ADR-011]], [[ADR-012]], [[ADR-014]]
