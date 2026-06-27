# Enterprise AI Platform — Diagram Index

All diagrams use a consistent visual language: white backgrounds, rounded rectangles, Segoe UI/Inter fonts, subtle colors, clean layouts.

## Colour Palette

| Colour | Hex | Usage |
|--------|-----|-------|
| Primary Blue | #2563EB | Platform, Interfaces, Actions |
| Secondary Purple | #7C3AED | AI/ML, Graph, Knowledge |
| Success Green | #059669 | Completion, Evaluation, Testing |
| Warning Orange | #D97706 | Optional, Vector, Async |
| Neutral Gray | #6B7280 | Infrastructure, Fallbacks |
| Database Cyan | #0891B2 | PostgreSQL, Persistence |
| LLM Pink | #DB2777 | Ollama, OpenAI, LLM Backends |

## Diagram Inventory

| # | Filename | Purpose | Referenced In |
|---|----------|---------|---------------|
| 01 | `01-system-context.svg` | C4 Level 1 — System landscape with external actors | Handbook §4.1 |
| 02 | `02-container-architecture.svg` | C4 Level 2 — Runtime containers and protocols | Handbook §4.2 |
| 03 | `03-module-dependencies.svg` | Maven module dependency graph with layers | Handbook §5.1 |
| 04 | `04-ai-inference-pipeline.svg` | Sequence: query → answer through full AI pipeline | Handbook §8.1 |
| 05 | `05-document-ingestion.svg` | Sequence: upload → extraction → enrichment → indexing | Handbook §7.1 |
| 06 | `06-semantic-enrichment.svg` | Data flow: document → entities → concepts → graph | Handbook §9.1 |
| 07 | `07-graphrag-retrieval.svg` | GraphRAG: 3-source fusion with Neo4j traversal | Handbook §10.1 |
| 08 | `08-retrieval-orchestration.svg` | Decision tree: intent → strategy → search | Handbook §11.1 |
| 09 | `09-hybrid-fusion.svg` | Score-level weighted linear fusion algorithm | Handbook §11.2 |
| 10 | `10-prompt-registry.svg` | Class diagram: PromptTemplate, Category, Registry | Handbook §12.1 |
| 11 | `11-model-capability-registry.svg` | Class diagram: ModelCapability, Registry, CapabilityRequest | Handbook §13.1 |
| 12 | `12-provider-router.svg` | Decision flow: 4-tier provider selection | Handbook §13.2 |
| 13 | `13-workflow-engine.svg` | State machine: 5-phase document intelligence workflow | Handbook §14.1 |
| 14 | `14-explainability.svg` | Metadata model: RetrievalOrchestrationResult fields | Handbook §15.1 |
| 15 | `15-observability.svg` | Metrics flow: AI operations → Micrometer → Prometheus | Handbook §16.1 |
| 16 | `16-deployment.svg` | C4 Deployment: containers, ports, protocols | Handbook §19.1 |
| 17 | `17-provider-spi.svg` | Provider SPI architecture: interfaces, implementations, fallbacks | Handbook §8.2 |
| 18 | `18-testing-pyramid.svg` | 5-layer testing strategy | Handbook §18.1 |
| 19 | `19-security.svg` | Authentication flow: JWT, BCrypt, refresh rotation | Handbook §17 |
| 20 | `20-repository-structure.svg` | Source tree: 9 modules, packages, resources | Handbook §5 |

## Visual Style Guide

- **Font**: Segoe UI, Inter, system-ui, sans-serif
- **Font sizes**: 18px titles, 13px headers, 12px body, 10px labels, 9px notes
- **Shapes**: Rounded rectangles (rx=6-8), clean straight lines
- **Spacing**: 12px internal padding, 24px between elements
- **Colors**: Subtle fills (10% opacity backgrounds), 100% strokes
- **No**: Gradients, drop shadows, clip art, decorative elements, raster images

## Regeneration

Diagrams are hand-authored SVG. To modify:
1. Edit the `.svg` file directly
2. Verify rendering by opening in a browser
3. Regenerate the handbook PDF with `npx md-to-pdf docs/Architecture-and-Engineering-Handbook-v3.md`

**Last Updated:** June 2026
