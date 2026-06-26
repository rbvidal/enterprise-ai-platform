-- V2__auth_tables.sql
-- Authentication: users, roles, and refresh token sessions

CREATE TABLE IF NOT EXISTS auth_users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL UNIQUE,
    display_name    VARCHAR(255),
    password_hash   VARCHAR(255) NOT NULL,
    enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_auth_users_email ON auth_users (email);

CREATE TABLE IF NOT EXISTS auth_user_roles (
    user_id UUID NOT NULL REFERENCES auth_users(id) ON DELETE CASCADE,
    role    VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role)
);

CREATE TABLE IF NOT EXISTS auth_refresh_token_sessions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES auth_users(id) ON DELETE CASCADE,
    token_hash      VARCHAR(255) NOT NULL,
    issued_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMPTZ NOT NULL,
    revoked         BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at      TIMESTAMPTZ,
    user_agent      VARCHAR(500),
    ip_address      VARCHAR(50)
);

CREATE INDEX IF NOT EXISTS idx_auth_refresh_token_hash ON auth_refresh_token_sessions (token_hash);
CREATE INDEX IF NOT EXISTS idx_auth_refresh_token_user ON auth_refresh_token_sessions (user_id);
