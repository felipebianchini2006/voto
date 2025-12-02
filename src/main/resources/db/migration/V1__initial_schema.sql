-- V1__initial_schema.sql
-- Initial database schema for Electronic Voting System
-- Author: Sistema de Votação Eletrônica
-- Date: 2025-12-01

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Create audit schema
CREATE SCHEMA IF NOT EXISTS audit;

-- ============================================================================
-- ELECTIONS TABLE
-- ============================================================================
CREATE TABLE elections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    start_ts TIMESTAMPTZ NOT NULL,
    end_ts TIMESTAMPTZ NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',

    -- Election configuration
    max_votes_per_voter INTEGER NOT NULL DEFAULT 1,
    allow_abstention BOOLEAN NOT NULL DEFAULT true,
    require_justification BOOLEAN NOT NULL DEFAULT false,

    -- Metadata
    created_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT chk_election_status CHECK (status IN ('DRAFT', 'RUNNING', 'CLOSED', 'CANCELLED')),
    CONSTRAINT chk_election_dates CHECK (end_ts > start_ts),
    CONSTRAINT chk_max_votes CHECK (max_votes_per_voter > 0)
);

CREATE INDEX idx_elections_status ON elections(status);
CREATE INDEX idx_elections_dates ON elections(start_ts, end_ts);
CREATE INDEX idx_elections_created_at ON elections(created_at);

COMMENT ON TABLE elections IS 'Main elections table';
COMMENT ON COLUMN elections.status IS 'DRAFT: being prepared, RUNNING: active voting, CLOSED: finished, CANCELLED: cancelled';

-- ============================================================================
-- CANDIDATES TABLE
-- ============================================================================
CREATE TABLE candidates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    election_id UUID NOT NULL REFERENCES elections(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    ballot_number INTEGER NOT NULL,
    description TEXT,
    photo_url VARCHAR(500),

    -- Metadata
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT uk_candidate_ballot_number UNIQUE (election_id, ballot_number),
    CONSTRAINT chk_ballot_number_positive CHECK (ballot_number > 0)
);

CREATE INDEX idx_candidates_election ON candidates(election_id);
CREATE INDEX idx_candidates_ballot_number ON candidates(election_id, ballot_number);

COMMENT ON TABLE candidates IS 'Candidates/options for each election';

-- ============================================================================
-- VOTERS TABLE
-- ============================================================================
CREATE TABLE voters (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    election_id UUID NOT NULL REFERENCES elections(id) ON DELETE CASCADE,

    -- Voter identification (hashed for privacy)
    external_id VARCHAR(255) NOT NULL,
    external_id_hash VARCHAR(64) NOT NULL,

    -- Contact
    email VARCHAR(255),
    email_hash VARCHAR(64),

    -- Eligibility
    eligible BOOLEAN NOT NULL DEFAULT true,
    ineligibility_reason TEXT,

    -- Metadata
    registered_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT uk_voter_external_id UNIQUE (election_id, external_id_hash),
    CONSTRAINT uk_voter_email UNIQUE (election_id, email_hash)
);

CREATE INDEX idx_voters_election ON voters(election_id);
CREATE INDEX idx_voters_external_id_hash ON voters(external_id_hash);
CREATE INDEX idx_voters_email_hash ON voters(email_hash);
CREATE INDEX idx_voters_eligible ON voters(election_id, eligible);

COMMENT ON TABLE voters IS 'Registered voters for each election';
COMMENT ON COLUMN voters.external_id_hash IS 'SHA-256 hash of external_id for privacy';
COMMENT ON COLUMN voters.email_hash IS 'SHA-256 hash of email for privacy';

-- ============================================================================
-- VOTE TOKENS TABLE
-- ============================================================================
CREATE TABLE vote_tokens (
    token UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    election_id UUID NOT NULL REFERENCES elections(id) ON DELETE CASCADE,
    voter_id UUID NOT NULL REFERENCES voters(id) ON DELETE CASCADE,

    -- Token lifecycle
    issued_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL,
    consumed BOOLEAN NOT NULL DEFAULT false,
    consumed_at TIMESTAMPTZ,

    -- Security
    token_signature BYTEA NOT NULL,
    nonce VARCHAR(64) NOT NULL UNIQUE,

    -- Constraints
    CONSTRAINT uk_voter_token UNIQUE (election_id, voter_id),
    CONSTRAINT chk_token_consumed CHECK (
        (consumed = false AND consumed_at IS NULL) OR
        (consumed = true AND consumed_at IS NOT NULL)
    ),
    CONSTRAINT chk_token_expiry CHECK (expires_at > issued_at)
);

CREATE INDEX idx_vote_tokens_election ON vote_tokens(election_id);
CREATE INDEX idx_vote_tokens_voter ON vote_tokens(voter_id);
CREATE INDEX idx_vote_tokens_consumed ON vote_tokens(election_id, consumed);
CREATE INDEX idx_vote_tokens_expires ON vote_tokens(expires_at);
CREATE INDEX idx_vote_tokens_nonce ON vote_tokens(nonce);

COMMENT ON TABLE vote_tokens IS 'Tokens issued to voters for casting votes';
COMMENT ON COLUMN vote_tokens.token_signature IS 'Digital signature of the token';
COMMENT ON COLUMN vote_tokens.nonce IS 'Unique nonce to prevent replay attacks';

-- ============================================================================
-- ENCRYPTED BALLOTS TABLE (Append-only)
-- ============================================================================
CREATE TABLE encrypted_ballots (
    id BIGSERIAL PRIMARY KEY,
    election_id UUID NOT NULL REFERENCES elections(id) ON DELETE CASCADE,

    -- Encrypted vote data
    ballot_ciphertext BYTEA NOT NULL,
    ballot_hash BYTEA NOT NULL,

    -- Encryption metadata
    encryption_algorithm VARCHAR(50) NOT NULL DEFAULT 'AES-256-GCM',
    key_id VARCHAR(100) NOT NULL,
    nonce BYTEA NOT NULL,

    -- Token reference (optional - can be purged for extra anonymity)
    token_id UUID,

    -- Timestamp (immutable)
    ts TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT chk_ballot_hash_length CHECK (LENGTH(ballot_hash) = 32)
);

CREATE INDEX idx_encrypted_ballots_election ON encrypted_ballots(election_id);
CREATE INDEX idx_encrypted_ballots_ts ON encrypted_ballots(election_id, ts);
CREATE INDEX idx_encrypted_ballots_hash ON encrypted_ballots(ballot_hash);

COMMENT ON TABLE encrypted_ballots IS 'Append-only table storing encrypted votes';
COMMENT ON COLUMN encrypted_ballots.ballot_ciphertext IS 'Encrypted vote payload';
COMMENT ON COLUMN encrypted_ballots.ballot_hash IS 'SHA-256 hash of ciphertext for integrity';
COMMENT ON COLUMN encrypted_ballots.token_id IS 'Optional reference to consumed token';

-- Prevent updates and deletes on encrypted_ballots (append-only)
CREATE OR REPLACE FUNCTION prevent_ballot_modification()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Modification of encrypted_ballots is not allowed. Table is append-only.';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_prevent_ballot_update
BEFORE UPDATE ON encrypted_ballots
FOR EACH ROW EXECUTE FUNCTION prevent_ballot_modification();

CREATE TRIGGER trg_prevent_ballot_delete
BEFORE DELETE ON encrypted_ballots
FOR EACH ROW EXECUTE FUNCTION prevent_ballot_modification();

-- ============================================================================
-- ELECTION RESULTS TABLE
-- ============================================================================
CREATE TABLE election_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    election_id UUID NOT NULL REFERENCES elections(id) ON DELETE CASCADE,
    candidate_id UUID REFERENCES candidates(id) ON DELETE SET NULL,

    -- Results
    vote_count INTEGER NOT NULL DEFAULT 0,
    percentage DECIMAL(5, 2),

    -- Special vote types
    is_null_vote BOOLEAN NOT NULL DEFAULT false,
    is_blank_vote BOOLEAN NOT NULL DEFAULT false,
    is_abstention BOOLEAN NOT NULL DEFAULT false,

    -- Metadata
    calculated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT uk_election_candidate_result UNIQUE (election_id, candidate_id),
    CONSTRAINT chk_vote_count_positive CHECK (vote_count >= 0),
    CONSTRAINT chk_percentage_range CHECK (percentage >= 0 AND percentage <= 100)
);

CREATE INDEX idx_results_election ON election_results(election_id);
CREATE INDEX idx_results_candidate ON election_results(candidate_id);

COMMENT ON TABLE election_results IS 'Computed results after tally';

-- ============================================================================
-- AUDIT LOG TABLE (with hash chain)
-- ============================================================================
CREATE TABLE audit.audit_log (
    id BIGSERIAL PRIMARY KEY,

    -- Event data
    event_type VARCHAR(100) NOT NULL,
    event_data JSONB NOT NULL,

    -- Hash chain
    entry_hash BYTEA NOT NULL,
    prev_hash BYTEA,

    -- Digital signature
    signature BYTEA NOT NULL,
    signer_key_id VARCHAR(100) NOT NULL,

    -- Metadata
    ts TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT chk_entry_hash_length CHECK (LENGTH(entry_hash) = 32),
    CONSTRAINT chk_prev_hash_length CHECK (prev_hash IS NULL OR LENGTH(prev_hash) = 32)
);

CREATE INDEX idx_audit_log_ts ON audit.audit_log(ts);
CREATE INDEX idx_audit_log_event_type ON audit.audit_log(event_type);
CREATE INDEX idx_audit_log_entry_hash ON audit.audit_log(entry_hash);
CREATE INDEX idx_audit_log_prev_hash ON audit.audit_log(prev_hash);

COMMENT ON TABLE audit.audit_log IS 'Immutable audit log with hash chain';
COMMENT ON COLUMN audit.audit_log.entry_hash IS 'SHA-256 hash of this entry';
COMMENT ON COLUMN audit.audit_log.prev_hash IS 'Hash of previous entry (forms chain)';
COMMENT ON COLUMN audit.audit_log.signature IS 'Digital signature of this entry';

-- Prevent modifications to audit log
CREATE OR REPLACE FUNCTION prevent_audit_modification()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Modification of audit_log is not allowed. Table is append-only.';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_prevent_audit_update
BEFORE UPDATE ON audit.audit_log
FOR EACH ROW EXECUTE FUNCTION prevent_audit_modification();

CREATE TRIGGER trg_prevent_audit_delete
BEFORE DELETE ON audit.audit_log
FOR EACH ROW EXECUTE FUNCTION prevent_audit_modification();

-- ============================================================================
-- AUDIT COMMITMENTS TABLE
-- ============================================================================
CREATE TABLE audit.commitments (
    id BIGSERIAL PRIMARY KEY,

    -- Commitment data
    root_hash BYTEA NOT NULL,
    last_entry_id BIGINT NOT NULL REFERENCES audit.audit_log(id),
    entry_count INTEGER NOT NULL,

    -- Metadata
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published_url TEXT,

    -- Constraints
    CONSTRAINT chk_root_hash_length CHECK (LENGTH(root_hash) = 32),
    CONSTRAINT chk_entry_count_positive CHECK (entry_count > 0)
);

CREATE INDEX idx_commitments_created_at ON audit.commitments(created_at);
CREATE INDEX idx_commitments_last_entry ON audit.commitments(last_entry_id);

COMMENT ON TABLE audit.commitments IS 'Periodic commitments of audit log state';
COMMENT ON COLUMN audit.commitments.root_hash IS 'Merkle root or hash of current chain state';

-- ============================================================================
-- ADMIN USERS TABLE (for OAuth2 integration)
-- ============================================================================
CREATE TABLE admin_users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    role VARCHAR(50) NOT NULL,

    -- Status
    enabled BOOLEAN NOT NULL DEFAULT true,

    -- Metadata
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_login_at TIMESTAMPTZ,

    -- Constraints
    CONSTRAINT chk_admin_role CHECK (role IN ('ADMIN', 'AUDITOR', 'OPERATOR'))
);

CREATE INDEX idx_admin_users_username ON admin_users(username);
CREATE INDEX idx_admin_users_email ON admin_users(email);
CREATE INDEX idx_admin_users_role ON admin_users(role);

COMMENT ON TABLE admin_users IS 'Administrative users with system access';

-- ============================================================================
-- UPDATED_AT TRIGGERS
-- ============================================================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_elections_updated_at
BEFORE UPDATE ON elections
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_candidates_updated_at
BEFORE UPDATE ON candidates
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- INITIAL DATA
-- ============================================================================

-- Insert default admin user (to be updated via OAuth2)
INSERT INTO admin_users (username, email, role) VALUES
    ('admin', 'admin@voto.local', 'ADMIN'),
    ('auditor', 'auditor@voto.local', 'AUDITOR');

-- ============================================================================
-- GRANTS
-- ============================================================================
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO voto_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO voto_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA audit TO voto_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA audit TO voto_user;

-- End of migration
