package com.votoeletronico.voto.domain.election;

/**
 * Election lifecycle status
 */
public enum ElectionStatus {
    /**
     * Election is being prepared, not yet open for voting
     */
    DRAFT,

    /**
     * Election is active and accepting votes
     */
    RUNNING,

    /**
     * Election has ended, votes are being tallied or already tallied
     */
    CLOSED,

    /**
     * Election was cancelled and is no longer valid
     */
    CANCELLED
}
