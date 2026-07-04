# Enterprise AI Platform

A modular, local-first platform for building production-grade AI applications that combine Large Language Models, Retrieval-Augmented Generation, hybrid retrieval, semantic search, knowledge graphs, and enterprise software architecture.

**Java 21 · Spring Boot 3.3 · 9 Modules · 176 Tests**

---

## What Is This?

The Enterprise AI Platform is a **reusable foundation** — not a single application. It provides the infrastructure, abstractions, and architectural patterns for building AI-powered enterprise applications: clear module boundaries, swappable provider backends, versioned prompts, automatic quality evaluation, and graceful degradation when infrastructure fails.

The repository currently contains one reference application built on the platform:

- **Document Intelligence** — Upload, index, enrich, search, and analyze document collections with AI-grounded answers and full explainability

---

## Platform vs Reference Applications

```
Enterprise AI Platform          ← Reusable core (this repository)
        │
        ├── Document Intelligence   ← First reference application
        │
        └── Future applications may include:
              Contract Intelligence
              Compliance Intelligence
              Engineering Knowledge
              Financial Intelligence
              Research Assistant
```

The platform provides **capabilities**. Reference applications demonstrate **use cases**. The platform is the product; the applications are examples of what you can build with it.

---

## Quick Start

```bash
git clone <repo-url>
cd enterprise-ai-platform

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
platform-api              Application assembly (REST + Thymeleaf UI)
    ↓
platform-ai               RAG orchestration, enrichment, evaluation, registries
platform-search           Hybrid search (keyword + vector + graph)
platform-document         Document lifecycle + ingestion pipeline
platform-neo4j            Knowledge graph persistence (auto-generated)
platform-workspace        Workspace wizard + timeline
platform-observability    Micrometer metrics + health indicators
platform-auth             JWT authentication + user management
    ↓
platform-audit            Immutable audit log (leaf module)
```

Dependency direction flows downward. `platform-api` depends on everything; nobody depends on `platform-api`. Compile-time boundaries enforced by Maven.

---

## Platform Capabilities

| Capability | Description |
|-----------|-------------|
| **AI Orchestration** | Multi-provider LLM integration (Ollama local, OpenAI-compatible cloud) behind a common SPI |
| **Prompt Registry** | Versioned prompts with categories, variable substitution, model compatibility, and audit trails |
| **Model Registry** | Queryable model capabilities — streaming, vision, JSON, embeddings, reasoning |
| **Provider Routing** | 4-tier deterministic provider selection based on model prefix, capability, preference, and availability |
| **Hybrid Retrieval** | Keyword (JPA/TF), vector (Qdrant), and graph (Neo4j) search with weighted linear fusion |
| **Semantic Enrichment** | Automatic entity, concept, and relationship extraction during document ingestion |
| **Knowledge Graph** | Auto-generated Neo4j graph with typed nodes, relationships, and full provenance |
| **GraphRAG** | Knowledge graph traversal augments retrieval with relationship-aware discovery |
| **Evaluation** | Automated grounding, faithfulness, and hallucination scoring on every answer |
| **Explainability** | Full inference metadata — provider, model, prompt version, strategy, timing, source counts |
| **Grounding** | Source reattribution, confidence profiling, and evidence-backed answer assembly |
| **Workflow Engine** | Configurable multi-step processes with registered phase handlers |
| **Observability** | Micrometer + Prometheus metrics, health indicators, correlation IDs |
| **Graceful Degradation** | Every external dependency is optional — the platform starts with only PostgreSQL |

---

## Documentation

| Book | Audience | Purpose |
|------|----------|---------|
| [Architecture Handbook](docs/Enterprise-AI-Platform-Architecture-Handbook.pdf) | Architects, Staff Engineers, Principal Engineers | Design philosophy, architecture, trade-offs, lessons learned |
| [Architecture & Engineering Handbook](docs/Enterprise-AI-Platform-Architecture-and-Engineering-Handbook.pdf) | Senior Engineers, Platform Engineers | Complete technical reference with diagrams |
| [Architecture Decision Records](docs/Enterprise-AI-Platform-Architecture-Decision-Records.pdf) | Architects, Principal Engineers | Permanent record of every major engineering decision |
| [Developer Guide](docs/Enterprise-AI-Platform-Developer-Guide.pdf) | Developers, Contributors | Build, run, extend, test, debug, contribute |

---

## Testing

```bash
mvn verify                    # 176 tests: unit + integration + architecture + contract
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

**[Apache License 2.0](LICENSE)**

---

> **For architects:** Start with the [Architecture Handbook](docs/Enterprise-AI-Platform-Architecture-Handbook.pdf). It explains *why* the platform was designed this way.
>
> **For developers:** Start with the [Developer Guide](docs/Enterprise-AI-Platform-Developer-Guide.pdf). It explains *how* to build, run, and extend the platform.
>
> **For decision rationale:** Consult the [ADR volume](docs/Enterprise-AI-Platform-Architecture-Decision-Records.pdf). It preserves the architectural history of the project.
