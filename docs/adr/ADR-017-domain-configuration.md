# ADR-017 — DomainConfiguration for Multi-Domain AI

## Status

Accepted. Interface defined in `platform-ai/src/main/java/com/cognitera/platform/ai/api/DomainConfiguration.java`. Default implementations embedded in existing services.

## Context

The platform must support multiple application domains (contract intelligence, financial analysis, regulatory compliance, technical documentation) from a single codebase. Domain-specific logic (concept definitions, analysis objectives, finding hierarchies, system instructions) must not be hardcoded in platform services. The platform core must remain domain-independent.

## Decision

Define a **`DomainConfiguration` SPI** that encapsulates domain-specific AI behavior:

| Method | Returns | Purpose |
|--------|---------|---------|
| `domainId()` / `displayName()` | String | Identity |
| `concepts()` | `List<ConceptDefinition>` | Keywords + governing references per concept |
| `objectives()` | `List<ObjectiveDefinition>` | Analysis objectives with keywords |
| `findingRoleMapping()` | `Map<String, String>` | Reference → finding role mapping |
| `findingRelationships()` | `List<FindingRelationship>` | Relationships between findings |
| `centralityWeights()` | `Map<String, Double>` | Centrality weights for references |
| `peripheralReferences()` | `Set<String>` | References classified as peripheral |
| `roleKeywords()` | `Map<String, List<String>>` | Source role classification keywords |
| `systemInstruction()` | `String` | Domain-specific AI system instruction |
| `answerStructureGuidance()` | `String` | Domain-specific answer structure |

Applications provide their `DomainConfiguration` as a Spring bean. The platform's existing services (`DefaultConceptExtractionService`, `DefaultObjectiveAnalysisService`, etc.) currently embed default configurations. These defaults represent the original document intelligence domain but should be migrated to `DomainConfiguration` implementations.

## Alternatives Considered

- **Hardcode domain logic in each service**: Rejected. Couples the platform to a single domain and prevents reuse.
- **Database-driven configuration**: Deferred. Adds infrastructure dependency. SPI-based configuration is simpler and testable.
- **Remove all domain logic from platform**: Rejected. The platform must demonstrate real AI behavior. DomainConfiguration keeps domain logic while making it swappable.

## Consequences

- **Extension point exists**: New domains implement `DomainConfiguration` without touching platform code
- **Migration path**: Existing hardcoded domain rules in 8 services can migrate to `DomainConfiguration` implementations incrementally
- **Testability**: Domain configurations can be tested independently

## Trade-offs

- Currently a forward-looking SPI — the 8 existing services still embed their configurations rather than consuming `DomainConfiguration`
- No multi-domain routing (which `DomainConfiguration` to use for a given query)
- No domain discovery mechanism

## Future Evolution

- Migrate 8 services to consume `DomainConfiguration`
- Multi-domain routing based on query classification
- Domain configuration discovery (`List<DomainConfiguration>` injection for multi-domain setups)
- External domain configuration via YAML/JSON files

See also: [[ADR-003]], [[ADR-007]]
