package com.votoeletronico.voto.domain.audit;

/**
 * Types of auditable events in the system
 */
public enum AuditEventType {
    // Election events
    ELECTION_CREATED,
    ELECTION_UPDATED,
    ELECTION_STARTED,
    ELECTION_CLOSED,
    ELECTION_CANCELLED,
    ELECTION_DELETED,

    // Candidate events
    CANDIDATE_ADDED,
    CANDIDATE_UPDATED,
    CANDIDATE_DELETED,

    // Voter events
    VOTER_REGISTERED,
    VOTER_UPDATED,
    VOTER_ELIGIBILITY_CHANGED,

    // Vote token events
    VOTE_TOKEN_ISSUED,
    VOTE_TOKEN_CONSUMED,

    // Voting events
    VOTE_CAST,
    VOTE_VERIFIED,

    // Tally events
    TALLY_STARTED,
    TALLY_COMPLETED,

    // Security events
    UNAUTHORIZED_ACCESS_ATTEMPT,
    AUTHENTICATION_FAILED,
    AUTHENTICATION_SUCCEEDED
}
