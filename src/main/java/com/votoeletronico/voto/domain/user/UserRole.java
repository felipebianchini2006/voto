package com.votoeletronico.voto.domain.user;

/**
 * User roles in the voting system.
 *
 * ADMIN: System administrators with full access
 * CANDIDATE: Candidates who can register for elections
 *
 * Note: Voters are NOT user accounts - they authenticate via passwordless tokens
 * for privacy preservation.
 */
public enum UserRole {
    /**
     * System administrator with full access to all functions
     */
    ADMIN,

    /**
     * Candidate who can self-register and apply to elections
     */
    CANDIDATE
}
