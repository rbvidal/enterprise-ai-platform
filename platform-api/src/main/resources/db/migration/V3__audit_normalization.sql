-- V3__audit_normalization.sql
-- Add normalization columns (idempotent)

ALTER TABLE audit_events ADD COLUMN IF NOT EXISTS source_module VARCHAR(100);
ALTER TABLE audit_events ADD COLUMN IF NOT EXISTS request_path VARCHAR(500);
ALTER TABLE audit_events ADD COLUMN IF NOT EXISTS http_method VARCHAR(10);

-- Recreate trigger (idempotent via DROP IF EXISTS in V1)
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
