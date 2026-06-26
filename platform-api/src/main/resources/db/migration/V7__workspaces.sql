-- V7__workspaces.sql
-- Workspace management, document links, timelines, and workflow steps

CREATE TABLE IF NOT EXISTS workspaces (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    workspace_type  VARCHAR(100) NOT NULL DEFAULT 'CUSTOM',
    phase           VARCHAR(50) NOT NULL DEFAULT 'SETUP',
    status          VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    owner_id        VARCHAR(255),
    stage_data      JSONB DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_workspaces_status ON workspaces (status);
CREATE INDEX IF NOT EXISTS idx_workspaces_owner ON workspaces (owner_id);

CREATE TABLE IF NOT EXISTS workspace_documents (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id    UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    document_id     UUID NOT NULL,
    document_name   VARCHAR(500),
    document_type   VARCHAR(100),
    classification  VARCHAR(50) DEFAULT 'FACTUAL',
    notes           TEXT,
    uploaded_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ws_docs_workspace ON workspace_documents (workspace_id);
CREATE INDEX IF NOT EXISTS idx_ws_docs_document ON workspace_documents (document_id);

CREATE TABLE IF NOT EXISTS workspace_timeline_events (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id        UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    event_date          DATE NOT NULL,
    title               VARCHAR(500) NOT NULL,
    description         TEXT,
    event_type          VARCHAR(100) NOT NULL DEFAULT 'OTHER',
    source_document_id  UUID,
    confidence          DOUBLE PRECISION DEFAULT 1.0,
    ai_generated        BOOLEAN DEFAULT FALSE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_timeline_ws ON workspace_timeline_events (workspace_id);
CREATE INDEX IF NOT EXISTS idx_timeline_date ON workspace_timeline_events (event_date);

CREATE TABLE IF NOT EXISTS workspace_steps (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id    UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    phase           VARCHAR(50) NOT NULL,
    step_name       VARCHAR(255),
    status          VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    completed_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ws_steps_workspace ON workspace_steps (workspace_id);
