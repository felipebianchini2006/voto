package com.votoeletronico.voto.domain.voting;

import com.votoeletronico.voto.domain.election.Election;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents an encrypted vote ballot.
 * This table is append-only (enforced by DB trigger) to ensure audit trail integrity.
 * Votes are encrypted and cannot be linked to voter identity.
 */
@Entity
@Table(name = "encrypted_ballots", indexes = {
        @Index(name = "idx_encrypted_ballots_election", columnList = "election_id"),
        @Index(name = "idx_encrypted_ballots_ballot_hash", columnList = "ballot_hash", unique = true),
        @Index(name = "idx_encrypted_ballots_cast_at", columnList = "cast_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EncryptedBallot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * The election this ballot belongs to
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "election_id", nullable = false)
    @NotNull
    private Election election;

    /**
     * Encrypted vote data (contains candidate selection)
     * Encrypted with election public key
     */
    @Column(name = "encrypted_vote", nullable = false, columnDefinition = "TEXT")
    @NotNull
    private String encryptedVote;

    /**
     * SHA-256 hash of the entire ballot (for verification)
     * Voters receive this as a receipt to verify their vote was counted
     */
    @Column(name = "ballot_hash", nullable = false, unique = true, length = 64)
    @NotNull
    private String ballotHash;

    /**
     * Encryption algorithm used (e.g., "RSA-2048+AES-256-GCM")
     */
    @Column(name = "encryption_algorithm", nullable = false)
    @NotNull
    private String encryptionAlgorithm;

    /**
     * Key ID used for encryption (for key rotation support)
     */
    @Column(name = "key_id", nullable = false)
    @NotNull
    private String keyId;

    /**
     * Initialization vector / nonce for encryption
     */
    @Column(name = "nonce", nullable = false)
    @NotNull
    private String nonce;

    /**
     * Timestamp when vote was cast
     */
    @Column(name = "cast_at", nullable = false, updatable = false)
    @NotNull
    private Instant castAt;

    /**
     * IP address hash (for audit/security, not for voter identification)
     */
    @Column(name = "ip_hash")
    private String ipHash;

    /**
     * User agent hash (for audit/security)
     */
    @Column(name = "user_agent_hash")
    private String userAgentHash;

    /**
     * Previous ballot hash in the chain (for integrity verification)
     */
    @Column(name = "prev_ballot_hash", length = 64)
    private String prevBallotHash;

    /**
     * Verification signature (proves ballot integrity)
     */
    @Column(name = "verification_signature", columnDefinition = "TEXT")
    private String verificationSignature;

    /**
     * Flag indicating if ballot has been counted in tally
     */
    @Column(name = "tallied", nullable = false)
    @Builder.Default
    private Boolean tallied = false;

    /**
     * Timestamp when ballot was included in tally
     */
    @Column(name = "tallied_at")
    private Instant talliedAt;

    @PrePersist
    protected void onCreate() {
        if (castAt == null) {
            castAt = Instant.now();
        }
    }

    /**
     * Mark ballot as tallied
     */
    public void markAsTallied() {
        this.tallied = true;
        this.talliedAt = Instant.now();
    }

    /**
     * Check if ballot is valid for tallying
     */
    public boolean canBeTallied() {
        return !tallied && encryptedVote != null && ballotHash != null;
    }
}
