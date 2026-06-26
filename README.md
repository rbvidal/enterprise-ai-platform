# Document Intelligence Platform

General-purpose document intelligence platform built with Java 21, Spring Boot 3.3, and Thymeleaf.

## Overview

An enterprise-grade platform for uploading, indexing, searching, and reasoning across document collections. Supports any domain: enterprise knowledge management, technical documentation, research papers, compliance documents, contracts, and more.

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│              Thymeleaf UI (Bootstrap 5)                  │
│  Dashboard │ Documents │ Search │ Workspaces │ AI       │
└────────────────────┬────────────────────────────────────┘
                     │ Server-side rendering + REST API
┌────────────────────┴────────────────────────────────────┐
│              Spring Boot 3.3 (Java 21)                   │
│                                                          │
│  platform-api       Page controllers, REST API, DTOs     │
│  platform-web       Thymeleaf templates, CSS, i18n      │
│  platform-ai        LLM orchestration, retrieval         │
│  platform-search    Hybrid retrieval, vector + keyword   │
│  platform-document  Ingestion, versioning, storage       │
│  platform-workspace Workspace management, timelines      │
│  platform-knowledge Knowledge base, taxonomy             │
│  platform-neo4j     Knowledge graph integration          │
│  platform-auth      JWT auth, RBAC                       │
│  platform-audit     Immutable audit event log            │
│  platform-domain    Shared domain interfaces             │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────┴────────────────────────────────────┐
│                   Infrastructure                         │
│  PostgreSQL  │  Qdrant  │  Neo4j  │  Ollama             │
└─────────────────────────────────────────────────────────┘
```

## Key Features

- **Document Ingestion:** PDF, DOCX, TXT, HTML parsing with chunking and embedding
- **Hybrid Search:** Semantic vector search + BM25 keyword search with fusion and reranking
- **AI-Powered Q&A:** Retrieval-augmented generation with source citations
- **Knowledge Graph:** Neo4j-powered entity and relationship exploration
- **Workspaces:** Stage-based workflow (Setup → Ingestion → Analysis → Review → Complete)
- **Audit Logging:** Immutable event log with correlation tracing
- **Multi-language:** i18n support (English, German, French)

## Quick Start

### Prerequisites

- Java 21
- Docker and Docker Compose
- Maven 3.9+

### Start Infrastructure

```bash
docker compose up -d
```

### Run

```bash
mvn spring-boot:run -pl platform-api
```

The application is available at http://localhost:8080

## Module Structure

| Module | Description |
|---|---|
| `platform-domain` | Shared domain interfaces |
| `platform-audit` | Immutable audit event infrastructure |
| `platform-auth` | JWT authentication, RBAC |
| `platform-document` | Document lifecycle, ingestion, versioning |
| `platform-search` | Hybrid retrieval, chunking |
| `platform-ai` | LLM orchestration, RAG |
| `platform-workspace` | Workspace management, stage-based workflows |
| `platform-knowledge` | Knowledge base, taxonomy |
| `platform-neo4j` | Knowledge graph (Neo4j) |
| `platform-web` | Thymeleaf templates, CSS, i18n |
| `platform-api` | Page controllers, REST API, application assembly |

## Technology Stack

- **Backend:** Java 21, Spring Boot 3.3, Spring Security, Spring AI, JPA/Hibernate
- **Vector Store:** Qdrant
- **Graph DB:** Neo4j 5
- **AI/ML:** Ollama (local LLM + embeddings), Spring AI abstraction
- **Frontend:** Thymeleaf, Bootstrap 5
- **Infrastructure:** Docker Compose, PostgreSQL 16

## License

MIT
