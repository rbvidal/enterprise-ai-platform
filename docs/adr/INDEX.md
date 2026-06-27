# Architecture Decision Records — Index

## ADR List

| ADR | Title | Status |
|-----|-------|--------|
| [ADR-001](ADR-001-modular-monolith.md) | Modular Monolith Architecture | Accepted |
| [ADR-002](ADR-002-spring-boot.md) | Spring Boot 3.3 as Application Framework | Accepted |
| [ADR-003](ADR-003-provider-abstraction.md) | Provider Abstraction via SPI | Accepted |
| [ADR-004](ADR-004-provider-router.md) | Provider Router for Intelligent Model Selection | Accepted |
| [ADR-005](ADR-005-prompt-registry.md) | Prompt Registry for Versioned Prompt Management | Accepted |
| [ADR-006](ADR-006-model-capability-registry.md) | Model Capability Registry | Accepted |
| [ADR-007](ADR-007-semantic-enrichment.md) | Semantic Enrichment Engine | Accepted |
| [ADR-008](ADR-008-graphrag.md) | GraphRAG — Graph-Enhanced Retrieval | Accepted |
| [ADR-009](ADR-009-neo4j.md) | Neo4j as Knowledge Graph Persistence | Accepted |
| [ADR-010](ADR-010-qdrant.md) | Qdrant as Vector Database | Accepted |
| [ADR-011](ADR-011-retrieval-orchestration.md) | Retrieval Orchestration with Intent-Based Strategy Selection | Accepted |
| [ADR-012](ADR-012-explainability.md) | Explainability by Default | Accepted |
| [ADR-013](ADR-013-evaluation.md) | Evaluation Engine | Accepted |
| [ADR-014](ADR-014-workflow-engine.md) | Workflow Engine | Accepted |
| [ADR-015](ADR-015-ai-observability.md) | AI Observability with Micrometer | Accepted |
| [ADR-016](ADR-016-graceful-degradation.md) | Graceful Degradation Over Hard Failures | Accepted |
| [ADR-017](ADR-017-domain-configuration.md) | DomainConfiguration for Multi-Domain AI | Accepted |
| [ADR-018](ADR-018-provenance-graph.md) | Provenance-Aware Knowledge Graph | Accepted |
| [ADR-019](ADR-019-testing-strategy.md) | Multi-Layer Testing Strategy | Accepted |
| [ADR-020](ADR-020-platform-philosophy.md) | Enterprise AI Platform Design Philosophy | Accepted |

## Decision Categories

### Architecture & Structure
- [ADR-001](ADR-001-modular-monolith.md) — Module boundaries
- [ADR-002](ADR-002-spring-boot.md) — Technology stack
- [ADR-020](ADR-020-platform-philosophy.md) — Governing principles

### AI Infrastructure
- [ADR-003](ADR-003-provider-abstraction.md) — Provider SPI design
- [ADR-004](ADR-004-provider-router.md) — Intelligent model routing
- [ADR-005](ADR-005-prompt-registry.md) — Versioned prompt management
- [ADR-006](ADR-006-model-capability-registry.md) — Model capability database

### Knowledge & Retrieval
- [ADR-007](ADR-007-semantic-enrichment.md) — Automatic knowledge extraction
- [ADR-008](ADR-008-graphrag.md) — Graph-enhanced retrieval
- [ADR-009](ADR-009-neo4j.md) — Graph database selection
- [ADR-010](ADR-010-qdrant.md) — Vector database selection
- [ADR-011](ADR-011-retrieval-orchestration.md) — Intent-driven retrieval
- [ADR-018](ADR-018-provenance-graph.md) — Auditable knowledge graph

### Quality & Operations
- [ADR-012](ADR-012-explainability.md) — Inference transparency
- [ADR-013](ADR-013-evaluation.md) — Quality assessment
- [ADR-015](ADR-015-ai-observability.md) — Operational metrics
- [ADR-016](ADR-016-graceful-degradation.md) — Resilience patterns

### Extensibility
- [ADR-014](ADR-014-workflow-engine.md) — Reusable process engine
- [ADR-017](ADR-017-domain-configuration.md) — Multi-domain support
- [ADR-019](ADR-019-testing-strategy.md) — Validation architecture

## Dependency Graph

```
ADR-020 (Philosophy) ──────────────────────────────────────────┐
    │                                                          │
    ├── ADR-001 (Modular Monolith) ─── ADR-002 (Spring Boot)   │
    │                                                          │
    ├── ADR-003 (Provider SPI)                                  │
    │       ├── ADR-004 (Provider Router)                       │
    │       │       └── ADR-006 (Model Registry)                │
    │       ├── ADR-005 (Prompt Registry)                       │
    │       └── ADR-016 (Graceful Degradation)                  │
    │                                                          │
    ├── ADR-007 (Semantic Enrichment)                           │
    │       ├── ADR-008 (GraphRAG)                              │
    │       │       ├── ADR-009 (Neo4j)                        │
    │       │       └── ADR-011 (Retrieval Orchestration)       │
    │       │               └── ADR-012 (Explainability)        │
    │       │                       └── ADR-013 (Evaluation)    │
    │       ├── ADR-010 (Qdrant)                                │
    │       └── ADR-018 (Provenance Graph)                      │
    │                                                          │
    ├── ADR-014 (Workflow Engine)                               │
    ├── ADR-015 (AI Observability)                              │
    ├── ADR-017 (Domain Configuration)                          │
    │                                                          │
    └── ADR-019 (Testing Strategy)                              │
```

## Reading Order

**For new team members:**

1. Start with [ADR-020](ADR-020-platform-philosophy.md) — understand why the platform exists
2. Read [ADR-001](ADR-001-modular-monolith.md) and [ADR-002](ADR-002-spring-boot.md) — understand the structure
3. Read [ADR-003](ADR-003-provider-abstraction.md) — understand the core SPI pattern
4. Then follow the dependency graph for your area of interest:
   - **AI Infra**: ADR-004 → ADR-005 → ADR-006
   - **Knowledge/Retrieval**: ADR-007 → ADR-008 → ADR-009 → ADR-010 → ADR-011
   - **Quality/Ops**: ADR-012 → ADR-013 → ADR-015 → ADR-016
   - **Extensibility**: ADR-014 → ADR-017 → ADR-019

**For senior reviewers:**

Read ADR-020 first, then all others in any order. Cross-references between ADRs are marked with `[[ADR-NNN]]` links.
