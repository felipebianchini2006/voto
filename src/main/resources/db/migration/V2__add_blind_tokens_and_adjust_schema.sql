-- V2__add_blind_tokens_and_adjust_schema.sql
-- Add blind_tokens table and adjust encrypted_ballots schema

-- ============================================================================
-- BLIND TOKENS TABLE (for anonymous voting)
-- ============================================================================
CREATE TABLE blind_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    election_id UUID NOT NULL REFERENCES elections(id) ON DELETE CASCADE,
    
    -- Voter hash (for duplicate prevention, no link to identity)
    voter_id_hash VARCHAR(64) NOT NULL,
    
    -- Token data
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    signature TEXT NOT NULL,
    
    -- Status and lifecycle
    status VARCHAR(20) NOT NULL DEFAULT 'ISSUED',
    issued_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    
    -- Security
    nonce VARCHAR(64) NOT NULL UNIQUE,
    
    -- Ballot reference (for audit only, does NOT link voter to vote content)
    ballot_id UUID,
    
    -- Base entity fields
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- Constraints
    CONSTRAINT chk_blind_token_status CHECK (status IN ('ISSUED', 'CONSUMED', 'EXPIRED', 'REVOKED'))
);

CREATE INDEX idx_blind_tokens_election ON blind_tokens(election_id);
CREATE INDEX idx_blind_tokens_status ON blind_tokens(status);
CREATE INDEX idx_blind_tokens_voter_hash ON blind_tokens(voter_id_hash);
CREATE INDEX idx_blind_tokens_nonce ON blind_tokens(nonce);

COMMENT ON TABLE blind_tokens IS 'Blind tokens for anonymous voting - no link between voter and vote';
COMMENT ON COLUMN blind_tokens.voter_id_hash IS 'Hash of voter ID for duplicate prevention only';
COMMENT ON COLUMN blind_tokens.token_hash IS 'Hash of actual token value for verification';

-- ============================================================================
-- ADJUST ENCRYPTED_BALLOTS TABLE - Drop and recreate with proper schema
-- ============================================================================

-- First, remove the triggers that prevent deletes
DROP TRIGGER IF EXISTS trg_prevent_ballot_update ON encrypted_ballots;
DROP TRIGGER IF EXISTS trg_prevent_ballot_delete ON encrypted_ballots;

-- Drop old table and recreate with correct schema
DROP TABLE IF EXISTS encrypted_ballots CASCADE;

CREATE TABLE encrypted_ballots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    election_id UUID NOT NULL REFERENCES elections(id) ON DELETE CASCADE,
    
    -- Encrypted vote data
    encrypted_vote TEXT NOT NULL,
    ballot_hash VARCHAR(64) NOT NULL UNIQUE,
    
    -- Encryption metadata
    encryption_algorithm VARCHAR(50) NOT NULL,
    key_id VARCHAR(100) NOT NULL,
    nonce VARCHAR(100) NOT NULL,
    
    -- Timestamp
    cast_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- Security audit fields
    ip_hash VARCHAR(64),
    user_agent_hash VARCHAR(64),
    
    -- Chain integrity
    prev_ballot_hash VARCHAR(64),
    verification_signature TEXT,
    
    -- Tally tracking
    tallied BOOLEAN NOT NULL DEFAULT false,
    tallied_at TIMESTAMPTZ
);

CREATE INDEX idx_encrypted_ballots_election ON encrypted_ballots(election_id);
CREATE INDEX idx_encrypted_ballots_ballot_hash ON encrypted_ballots(ballot_hash);
CREATE INDEX idx_encrypted_ballots_cast_at ON encrypted_ballots(cast_at);

COMMENT ON TABLE encrypted_ballots IS 'Append-only table storing encrypted votes';

-- Recreate append-only triggers
CREATE TRIGGER trg_prevent_ballot_update
BEFORE UPDATE ON encrypted_ballots
FOR EACH ROW EXECUTE FUNCTION prevent_ballot_modification();

CREATE TRIGGER trg_prevent_ballot_delete
BEFORE DELETE ON encrypted_ballots
FOR EACH ROW EXECUTE FUNCTION prevent_ballot_modification();

-- Trigger for blind_tokens updated_at
CREATE TRIGGER trg_blind_tokens_updated_at
BEFORE UPDATE ON blind_tokens
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- End of migration
