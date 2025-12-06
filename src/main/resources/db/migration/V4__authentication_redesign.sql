-- V4__authentication_redesign.sql
-- Redesign authentication system with local JWT and candidate user accounts

-- ============================================================================
-- DROP OLD OAUTH2 ADMIN TABLE
-- ============================================================================

-- Backup existing admin users (if needed for reference)
CREATE TABLE IF NOT EXISTS admin_users_backup AS SELECT * FROM admin_users;

-- Drop old unused table
DROP TABLE IF EXISTS admin_users CASCADE;

-- ============================================================================
-- USERS TABLE (Local authentication for admins and candidates)
-- ============================================================================
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Authentication
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,

    -- Role
    role VARCHAR(20) NOT NULL,

    -- Account status
    enabled BOOLEAN NOT NULL DEFAULT true,
    account_locked BOOLEAN NOT NULL DEFAULT false,
    account_expired BOOLEAN NOT NULL DEFAULT false,
    credentials_expired BOOLEAN NOT NULL DEFAULT false,

    -- Password management
    password_change_required BOOLEAN NOT NULL DEFAULT false,
    last_password_change_at TIMESTAMPTZ,

    -- Login tracking
    last_login_at TIMESTAMPTZ,
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    locked_at TIMESTAMPTZ,

    -- Metadata
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT chk_user_role CHECK (role IN ('ADMIN', 'CANDIDATE'))
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_enabled ON users(enabled);

COMMENT ON TABLE users IS 'System users - admins and candidates with login capability';
COMMENT ON COLUMN users.password_hash IS 'BCrypt hashed password (strength 10)';
COMMENT ON COLUMN users.role IS 'User role: ADMIN or CANDIDATE';
COMMENT ON COLUMN users.failed_login_attempts IS 'Count of consecutive failed login attempts';
COMMENT ON COLUMN users.account_locked IS 'Account locked after 5 failed login attempts';

-- ============================================================================
-- UPDATE CANDIDATES TABLE - Link to User Account
-- ============================================================================

-- Add user_id foreign key to candidates
ALTER TABLE candidates ADD COLUMN user_id UUID;
ALTER TABLE candidates ADD COLUMN party VARCHAR(100);

-- Add foreign key constraint
ALTER TABLE candidates
    ADD CONSTRAINT fk_candidate_user
    FOREIGN KEY (user_id)
    REFERENCES users(id)
    ON DELETE SET NULL;

-- Create index for user lookup
CREATE INDEX idx_candidates_user_id ON candidates(user_id);

-- Add unique constraint: one user can only be one candidate per election
CREATE UNIQUE INDEX uk_candidate_user_election ON candidates(user_id, election_id)
    WHERE user_id IS NOT NULL;

COMMENT ON COLUMN candidates.user_id IS 'Link to user account if candidate self-registered (nullable for backward compatibility)';
COMMENT ON COLUMN candidates.party IS 'Political party or affiliation (optional)';

-- ============================================================================
-- UPDATED_AT TRIGGER FOR USERS TABLE
-- ============================================================================
CREATE TRIGGER trg_users_updated_at
BEFORE UPDATE ON users
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- BUSINESS RULE TRIGGERS
-- ============================================================================

-- Trigger 1: Enforce max 10 candidates per election
CREATE OR REPLACE FUNCTION check_candidate_limit()
RETURNS TRIGGER AS $$
DECLARE
    candidate_count INTEGER;
BEGIN
    -- Count current candidates for this election (excluding current record if updating)
    SELECT COUNT(*) INTO candidate_count
    FROM candidates
    WHERE election_id = NEW.election_id
    AND id != COALESCE(NEW.id, '00000000-0000-0000-0000-000000000000'::UUID);

    -- Check if limit reached
    IF candidate_count >= 10 THEN
        RAISE EXCEPTION 'Election has reached maximum of 10 candidates (current: %)', candidate_count;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_check_candidate_limit
BEFORE INSERT OR UPDATE ON candidates
FOR EACH ROW EXECUTE FUNCTION check_candidate_limit();

COMMENT ON FUNCTION check_candidate_limit() IS 'Enforce 10 candidate limit per election';

-- Trigger 2: Ensure candidate participates in only one RUNNING election at a time
CREATE OR REPLACE FUNCTION check_candidate_active_election()
RETURNS TRIGGER AS $$
DECLARE
    active_election_count INTEGER;
    new_election_status VARCHAR(20);
BEGIN
    -- Only check if user_id is set (self-registered candidates)
    IF NEW.user_id IS NULL THEN
        RETURN NEW;
    END IF;

    -- Get the status of the election being applied to
    SELECT status INTO new_election_status
    FROM elections
    WHERE id = NEW.election_id;

    -- Count active (RUNNING) elections for this user (excluding current record if updating)
    SELECT COUNT(*) INTO active_election_count
    FROM candidates c
    JOIN elections e ON c.election_id = e.id
    WHERE c.user_id = NEW.user_id
    AND e.status = 'RUNNING'
    AND c.id != COALESCE(NEW.id, '00000000-0000-0000-0000-000000000000'::UUID);

    -- Check if already in an active election
    IF active_election_count > 0 AND new_election_status = 'RUNNING' THEN
        RAISE EXCEPTION 'Candidate can only participate in one active (RUNNING) election at a time (currently in % active elections)', active_election_count;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_check_candidate_active_election
BEFORE INSERT OR UPDATE ON candidates
FOR EACH ROW EXECUTE FUNCTION check_candidate_active_election();

COMMENT ON FUNCTION check_candidate_active_election() IS 'Ensure candidate participates in only one RUNNING election at a time';

-- ============================================================================
-- SEED DEFAULT ADMIN ACCOUNT
-- ============================================================================

-- Generate BCrypt hash for password "admin123" with strength 10
-- BCrypt hash: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
-- Password: admin123 (MUST be changed in production!)

INSERT INTO users (username, password_hash, email, role, enabled, password_change_required)
VALUES (
    'admin',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'admin@voto.local',
    'ADMIN',
    true,
    false
) ON CONFLICT (username) DO NOTHING;

COMMENT ON TABLE users IS 'Default admin account created: username=admin, password=admin123';

-- ============================================================================
-- GRANTS
-- ============================================================================
DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_roles WHERE rolname = 'voto_user') THEN
        GRANT ALL PRIVILEGES ON users TO voto_user;
        GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO voto_user;
    END IF;
END $$;
