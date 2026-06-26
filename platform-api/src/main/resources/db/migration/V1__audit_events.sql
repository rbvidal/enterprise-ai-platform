-- V1__audit_events.sql
-- Immutable audit event log with JSONB metadata

CREATE TABLE IF NOT EXISTS audit_events (
    id              BIGSERIAL PRIMARY KEY,
    event_type      VARCHAR(100) NOT NULL,
    timestamp       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actor_id        VARCHAR(255),
    tenant_id       VARCHAR(255),
    entity_type     VARCHAR(100),
    entity_id       VARCHAR(255),
    correlation_id  VARCHAR(100),
    request_id      VARCHAR(100),
    request_path    VARCHAR(500),
    http_method     VARCHAR(10),
    source_module   VARCHAR(100),
    metadata        JSONB DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_events_timestamp ON audit_events (timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_audit_events_actor ON audit_events (actor_id);
CREATE INDEX IF NOT EXISTS idx_audit_events_type ON audit_events (event_type);
CREATE INDEX IF NOT EXISTS idx_audit_events_entity ON audit_events (entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_events_correlation ON audit_events (correlation_id);
CREATE INDEX IF NOT EXISTS idx_audit_events_request ON audit_events (request_id);
CREATE INDEX IF NOT EXISTS idx_audit_events_module ON audit_events (source_module);

-- Prevent UPDATE and DELETE on audit events (immutable log)
CREATE OR REPLACE FUNCTION prevent_audit_mutation()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'UPDATE' OR TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'Audit events are immutable: % operation not allowed', TG_OP;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_audit_events_no_mutation ON audit_events;
CREATE TRIGGER trg_audit_events_no_mutation
    BEFORE UPDATE OR DELETE ON audit_events
    FOR EACH ROW EXECUTE FUNCTION prevent_audit_mutation();
