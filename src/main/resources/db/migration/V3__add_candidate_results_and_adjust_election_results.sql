-- V3__add_candidate_results_and_adjust_election_results.sql
-- Add candidate_results table and adjust election_results to match entities

-- ============================================================================
-- DROP AND RECREATE ELECTION_RESULTS WITH PROPER SCHEMA
-- ============================================================================
DROP TABLE IF EXISTS election_results CASCADE;

CREATE TABLE election_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    election_id UUID NOT NULL REFERENCES elections(id) ON DELETE CASCADE,
    
    -- Status
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    
    -- Timestamps
    tally_started_at TIMESTAMPTZ,
    tally_completed_at TIMESTAMPTZ,
    
    -- Vote counts
    total_ballots BIGINT NOT NULL DEFAULT 0,
    valid_votes BIGINT NOT NULL DEFAULT 0,
    abstentions BIGINT NOT NULL DEFAULT 0,
    invalid_votes BIGINT NOT NULL DEFAULT 0,
    tokens_issued BIGINT NOT NULL DEFAULT 0,
    turnout_percentage DOUBLE PRECISION NOT NULL DEFAULT 0,
    
    -- Integrity
    merkle_root VARCHAR(64),
    results_hash VARCHAR(64),
    results_signature TEXT,
    
    -- Audit
    tallied_by UUID,
    published BOOLEAN NOT NULL DEFAULT false,
    published_at TIMESTAMPTZ,
    notes TEXT,
    
    -- Base entity fields
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- Constraints 
    CONSTRAINT uk_election_results_election UNIQUE (election_id),
    CONSTRAINT chk_election_results_status CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'VERIFIED', 'FAILED'))
);

CREATE INDEX idx_election_results_election ON election_results(election_id);
CREATE INDEX idx_election_results_status ON election_results(status);

COMMENT ON TABLE election_results IS 'Aggregated election results after tally';

-- ============================================================================
-- CANDIDATE_RESULTS TABLE
-- ============================================================================
CREATE TABLE candidate_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    election_result_id UUID NOT NULL REFERENCES election_results(id) ON DELETE CASCADE,
    candidate_id UUID NOT NULL REFERENCES candidates(id) ON DELETE CASCADE,
    
    -- Vote data
    vote_count BIGINT NOT NULL DEFAULT 0,
    percentage DOUBLE PRECISION NOT NULL DEFAULT 0,
    rank_position INTEGER,
    is_winner BOOLEAN NOT NULL DEFAULT false,
    
    -- Base entity fields
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_candidate_results_election_result ON candidate_results(election_result_id);
CREATE INDEX idx_candidate_results_candidate ON candidate_results(candidate_id);

COMMENT ON TABLE candidate_results IS 'Per-candidate vote counts and rankings';

-- ============================================================================
-- TRIGGERS FOR UPDATED_AT
-- ============================================================================

CREATE TRIGGER trg_election_results_updated_at
BEFORE UPDATE ON election_results
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_candidate_results_updated_at
BEFORE UPDATE ON candidate_results
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- End of migration
