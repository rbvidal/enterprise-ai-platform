# ADR-001 — Modular Monolith Architecture

## Status

Accepted. Implemented in the Maven module structure at `pom.xml`.

## Context

The Enterprise AI Platform must support multiple AI-powered applications (document intelligence, contract analysis, financial review, compliance, etc.) while remaining deployable as a single process. The platform must demonstrate clean separation of concerns without the operational complexity of microservices.

## Decision

Adopt a **Modular Monolith** architecture with 9 Maven modules:

```
platform-audit       — Immutable audit log, correlation IDs
platform-auth        — JWT authentication, refresh token rotation
platform-document    — Document lifecycle, ingestion pipeline
platform-search      — Hybrid search (keyword + vector + graph)
platform-ai          — RAG orchestration, enrichment, evaluation, registry
platform-neo4j       — Auto-generated knowledge graph with provenance
platform-workspace   — Multi-phase workflow wizard
platform-observability — Micrometer metrics, health indicators
platform-api         — REST + Thymeleaf controllers, assembly
```

Module boundaries follow **dependency inversion**: higher-level modules depend on lower-level APIs, never the reverse. `platform-api` is the assembly module that wires everything together.

## Alternatives Considered

- **Microservices**: Rejected. The platform demonstrates architecture without distributed systems complexity. Microservices add network boundaries, eventual consistency challenges, and deployment overhead that don't benefit a reference implementation.
- **Single JAR without modules**: Rejected. A flat structure would not enforce dependency direction or demonstrate separation of concerns.
- **OSGi modules**: Rejected. Adds runtime modularity complexity. Maven's compile-time enforcement is sufficient.

## Consequences

- **Clear dependency direction**: `api → ai → search → document → audit` (audit is a leaf dependency)
- **Compile-time enforcement**: Modules cannot accidentally depend on each other
- **Single deployable**: One `platform-api` Spring Boot application with all modules on the classpath
- **Test isolation**: Each module can be tested independently

## Trade-offs

- Module boundaries are enforced only at compile time, not runtime
- Adding a new module requires POM maintenance
- Some modules (audit) are cross-cutting dependencies of many others

## Future Evolution

- Modules may split if responsibilities grow too large (e.g., `platform-ai` could become `platform-ai-core` + `platform-ai-providers`)
- Spring Modulith could be introduced for runtime module verification
- The architecture deliberately supports future extraction of modules into microservices if needed

See also: [[ADR-002]], [[ADR-019]], [[ADR-020]]
