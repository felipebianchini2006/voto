package com.votoeletronico.voto.domain.results;

/**
 * Status of election tally process
 */
public enum TallyStatus {
    /**
     * Tally not yet started
     */
    PENDING,

    /**
     * Tally in progress
     */
    IN_PROGRESS,

    /**
     * Tally completed successfully
     */
    COMPLETED,

    /**
     * Tally failed (error during processing)
     */
    FAILED,

    /**
     * Results verified by auditors
     */
    VERIFIED
}
