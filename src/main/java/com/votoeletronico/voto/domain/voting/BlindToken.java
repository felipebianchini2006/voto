package com.votoeletronico.voto.domain.voting;

import com.votoeletronico.voto.domain.BaseEntity;
import com.votoeletronico.voto.domain.election.Election;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a blind token issued to an eligible voter.
 * Tokens provide anonymous voting capability - they can be verified without revealing voter identity.
 */
@Entity
@Table(name = "blind_tokens", indexes = {
        @Index(name = "idx_blind_tokens_election", columnList = "election_id"),
        @Index(name = "idx_blind_tokens_status", columnList = "status"),
        @Index(name = "idx_blind_tokens_token_hash", columnList = "token_hash", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlindToken extends BaseEntity {

    /**
     * The election this token is valid for
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "election_id", nullable = false)
    @NotNull
    private Election election;

    /**
     * Hash of the voter's external ID (for duplicate prevention only)
     * This is NOT exposed and does NOT link to the actual token
     */
    @Column(name = "voter_id_hash", nullable = false)
    @NotNull
    private String voterIdHash;

    /**
     * SHA-256 hash of the actual token value
     * Used for verification without storing plaintext token
     */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    @NotNull
    private String tokenHash;

    /**
     * Digital signature of the token (signed by election authority)
     * Proves authenticity without revealing identity
     */
    @Column(name = "signature", nullable = false, columnDefinition = "TEXT")
    @NotNull
    private String signature;

    /**
     * Token status: ISSUED, CONSUMED, EXPIRED, REVOKED
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @NotNull
    @Builder.Default
    private TokenStatus status = TokenStatus.ISSUED;

    /**
     * Timestamp when token was issued
     */
    @Column(name = "issued_at", nullable = false)
    @NotNull
    private Instant issuedAt;

    /**
     * Timestamp when token expires (typically election end time)
     */
    @Column(name = "expires_at", nullable = false)
    @NotNull
    private Instant expiresAt;

    /**
     * Timestamp when token was consumed (used to cast a vote)
     */
    @Column(name = "consumed_at")
    private Instant consumedAt;

    /**
     * Nonce for replay attack prevention
     */
    @Column(name = "nonce", nullable = false, unique = true)
    @NotNull
    private String nonce;

    /**
     * ID of the ballot if token was consumed (for audit purposes only)
     * Does NOT create a link between voter and vote content
     */
    @Column(name = "ballot_id")
    private UUID ballotId;

    // Business methods

    /**
     * Check if token is valid for voting
     */
    public boolean isValid() {
        return status == TokenStatus.ISSUED
                && Instant.now().isBefore(expiresAt);
    }

    /**
     * Check if token is expired
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    /**
     * Mark token as consumed
     */
    public void consume(UUID ballotId) {
        if (!isValid()) {
            throw new IllegalStateException("Cannot consume invalid token");
        }
        this.status = TokenStatus.CONSUMED;
        this.consumedAt = Instant.now();
        this.ballotId = ballotId;
    }

    /**
     * Revoke token (admin action)
     */
    public void revoke() {
        if (status == TokenStatus.CONSUMED) {
            throw new IllegalStateException("Cannot revoke consumed token");
        }
        this.status = TokenStatus.REVOKED;
    }

    /**
     * Update status based on expiration
     */
    @PrePersist
    @PreUpdate
    public void updateStatus() {
        if (status == TokenStatus.ISSUED && isExpired()) {
            this.status = TokenStatus.EXPIRED;
        }
    }
}
