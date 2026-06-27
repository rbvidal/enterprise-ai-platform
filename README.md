# Enterprise AI Platform

A reusable modular monolith for building AI-powered enterprise applications — retrieval-augmented generation, semantic enrichment, knowledge graph construction, multi-provider AI orchestration, and production observability.

**Java 21 · Spring Boot 3.3 · 9 Modules · 157 Tests**

---

## What Is This?

This is not a chatbot. It is not a RAG demo. It is a **reference implementation** demonstrating how an experienced engineering team integrates AI into enterprise software: clear module boundaries, swappable provider backends, versioned prompts, automatic quality evaluation, and graceful degradation when infrastructure fails.

The platform can serve as the foundation for document intelligence, contract analysis, enterprise search, compliance, financial review, and research platforms. The included Document Intelligence application is the first reference implementation.

---

## Quick Start

```bash
git clone <repo-url>
cd document-intelligence-platform

# Start infrastructure (PostgreSQL + Qdrant)
docker compose up -d

# Optional: Neo4j for GraphRAG
docker compose --profile graph up -d

# Build and run
mvn spring-boot:run -pl platform-api
```

Open `http://localhost:8080`. Upload documents, search, and query AI.

---

## Architecture

```
platform-api          Application assembly (REST + Thymeleaf UI)
    ↓
platform-ai           RAG orchestration, enrichment, evaluation, registries
platform-search       Hybrid search (keyword + vector + graph)
platform-document     Document lifecycle + ingestion pipeline
platform-neo4j        Knowledge graph persistence (auto-generated)
platform-workspace    Workspace wizard + timeline
platform-observability Micrometer metrics + health indicators
platform-auth         JWT authentication + user management
    ↓
platform-audit        Immutable audit log (leaf module)
```

**Dependency direction flows downward.** `platform-api` depends on everything; nobody depends on `platform-api`. Compile-time boundaries enforced by Maven.

---

## Key Capabilities

| Capability | Description |
|-----------|-------------|
| **Hybrid Search** | Keyword (JPA/TF) + Vector (Qdrant) + Graph (Neo4j) with weighted fusion |
| **GraphRAG** | Knowledge graph traversal augments retrieval when Neo4j is available |
| **Semantic Enrichment** | Automatic entity, concept, and relationship extraction during ingestion |
| **Multi-Provider AI** | Ollama (local) and OpenAI-compatible (cloud) behind a common SPI |
| **Prompt Registry** | Versioned prompts with categories, variables, and audit trails |
| **Model Registry** | Queryable model capabilities — streaming, vision, JSON, embeddings |
| **Provider Router** | 4-tier deterministic provider selection |
| **Evaluation** | Automated grounding, faithfulness, and hallucination scoring |
| **Explainability** | Full metadata on every inference — provider, model, prompt, strategy, timing |
| **Workflow Engine** | Configurable multi-step processes for document intelligence |
| **Observability** | Micrometer + Prometheus metrics, health indicators, correlation IDs |
| **Graceful Degradation** | Every external dependency is optional — the platform starts with only PostgreSQL |

---

## Documentation

| Book | Audience | Purpose |
|------|----------|---------|
| [Architecture & Engineering Handbook](docs/Architecture-Handbook.pdf) | Architects, Staff Engineers, Principal Engineers | Design philosophy, architecture, trade-offs, lessons learned |
| [Architecture Decision Records](docs/Architecture-Decision-Records.pdf) | Architects, Principal Engineers | Permanent record of every major engineering decision |
| [Developer Guide](docs/Developer-Guide.pdf) | Developers, Contributors | Build, run, extend, test, debug, contribute |

---

## Testing

```bash
mvn verify                    # 157 tests: unit + integration + architecture + resilience
mvn verify -Pui-tests         # + Playwright browser tests
```

---

## Requirements

- Java 21
- Maven 3.x
- Docker (for PostgreSQL, Qdrant, Neo4j)
- Ollama (optional — for local LLM inference)

---

## License

[Specify license]

---

> **For architects:** Start with the [Architecture Handbook](docs/Architecture-Handbook.pdf). It explains *why* the platform was designed this way.
>
> **For developers:** Start with the [Developer Guide](docs/Developer-Guide.pdf). It explains *how* to build, run, and extend the platform.
>
> **For decision rationale:** Consult the [ADR volume](docs/Architecture-Decision-Records.pdf). It preserves the architectural history of the project.
