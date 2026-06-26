-- V5__search_chunks.sql
-- Search: document chunks with metadata and tags

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE IF NOT EXISTS search_document_chunks (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id       UUID NOT NULL,
    document_version  INT NOT NULL,
    chunk_type        VARCHAR(50) NOT NULL DEFAULT 'TEXT',
    text              TEXT NOT NULL,
    page_number       INT,
    section_index     INT,
    chunk_index       INT,
    start_offset      INT,
    end_offset        INT,
    title             VARCHAR(500),
    document_type     VARCHAR(50),
    category          VARCHAR(100),
    source            VARCHAR(255),
    tenant_id         VARCHAR(255),
    document_created_at TIMESTAMPTZ,
    embedding_ref     VARCHAR(255),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_chunks_document ON search_document_chunks (document_id);
CREATE INDEX IF NOT EXISTS idx_chunks_document_version ON search_document_chunks (document_id, document_version);
CREATE INDEX IF NOT EXISTS idx_chunks_type ON search_document_chunks (chunk_type);
CREATE INDEX IF NOT EXISTS idx_chunks_text_trgm ON search_document_chunks USING gin (text gin_trgm_ops);

CREATE TABLE IF NOT EXISTS search_document_chunk_tags (
    chunk_id UUID NOT NULL REFERENCES search_document_chunks(id) ON DELETE CASCADE,
    tag      VARCHAR(255) NOT NULL,
    PRIMARY KEY (chunk_id, tag)
);

CREATE INDEX IF NOT EXISTS idx_chunk_tags_tag ON search_document_chunk_tags (tag);

CREATE TABLE IF NOT EXISTS search_document_chunk_metadata (
    chunk_id UUID NOT NULL REFERENCES search_document_chunks(id) ON DELETE CASCADE,
    key      VARCHAR(255) NOT NULL,
    value    TEXT,
    PRIMARY KEY (chunk_id, key)
);
