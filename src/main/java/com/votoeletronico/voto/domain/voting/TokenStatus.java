package com.votoeletronico.voto.domain.voting;

/**
 * Status of a blind token throughout its lifecycle
 */
public enum TokenStatus {
    /**
     * Token has been issued and is ready to use
     */
    ISSUED,

    /**
     * Token has been used to cast a vote
     */
    CONSUMED,

    /**
     * Token has expired (past election end time)
     */
    EXPIRED,

    /**
     * Token has been revoked by admin
     */
    REVOKED
}
