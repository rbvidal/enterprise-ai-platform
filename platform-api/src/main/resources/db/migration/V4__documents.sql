-- V4__documents.sql
-- Document management: documents, versions, tags, and ingestion jobs

CREATE TABLE IF NOT EXISTS documents (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       VARCHAR(255),
    title           VARCHAR(500),
    type            VARCHAR(50) NOT NULL DEFAULT 'OTHER',
    category        VARCHAR(100),
    status          VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    visibility      VARCHAR(50) NOT NULL DEFAULT 'PRIVATE',
    current_version INT NOT NULL DEFAULT 0,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_documents_tenant ON documents (tenant_id);
CREATE INDEX IF NOT EXISTS idx_documents_status ON documents (status);
CREATE INDEX IF NOT EXISTS idx_documents_type ON documents (type);
CREATE INDEX IF NOT EXISTS idx_documents_category ON documents (category);
CREATE INDEX IF NOT EXISTS idx_documents_created_at ON documents (created_at DESC);

CREATE TABLE IF NOT EXISTS document_tags (
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    tag         VARCHAR(255) NOT NULL,
    PRIMARY KEY (document_id, tag)
);

CREATE INDEX IF NOT EXISTS idx_document_tags_tag ON document_tags (tag);

CREATE TABLE IF NOT EXISTS document_versions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id     UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    version_number  INT NOT NULL,
    file_name       VARCHAR(500),
    content_type    VARCHAR(100),
    size_bytes      BIGINT,
    storage_provider VARCHAR(50),
    storage_key     VARCHAR(500),
    checksum_sha256 VARCHAR(64),
    created_by      VARCHAR(255),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (document_id, version_number)
);

CREATE INDEX IF NOT EXISTS idx_document_versions_doc ON document_versions (document_id);

CREATE TABLE IF NOT EXISTS document_ingestion_jobs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id     UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    version_number  INT NOT NULL,
    sequence_number INT,
    status          VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    requested_by    VARCHAR(255),
    started_at      TIMESTAMPTZ,
    completed_at    TIMESTAMPTZ,
    failed_at       TIMESTAMPTZ,
    failure_reason  TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ingestion_jobs_doc ON document_ingestion_jobs (document_id);
CREATE INDEX IF NOT EXISTS idx_ingestion_jobs_status ON document_ingestion_jobs (status);
