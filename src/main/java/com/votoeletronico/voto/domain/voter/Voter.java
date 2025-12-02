package com.votoeletronico.voto.domain.voter;

import com.votoeletronico.voto.domain.election.Election;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.apache.commons.codec.digest.DigestUtils;

import java.time.Instant;
import java.util.UUID;

/**
 * Voter entity representing a registered voter in an election
 * Privacy-focused: stores hashed identifiers only
 */
@Entity
@Table(name = "voters",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_voter_external_id",
                        columnNames = {"election_id", "external_id_hash"}),
                @UniqueConstraint(name = "uk_voter_email",
                        columnNames = {"election_id", "email_hash"})
        },
        indexes = {
                @Index(name = "idx_voters_election", columnList = "election_id"),
                @Index(name = "idx_voters_external_id_hash", columnList = "external_id_hash"),
                @Index(name = "idx_voters_email_hash", columnList = "email_hash"),
                @Index(name = "idx_voters_eligible", columnList = "election_id, eligible")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Voter {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "election_id", nullable = false, foreignKey = @ForeignKey(name = "fk_voter_election"))
    @JsonIgnore
    private Election election;

    @NotBlank(message = "External ID is required")
    @Size(max = 255)
    @Column(name = "external_id", nullable = false)
    private String externalId;

    @NotBlank(message = "External ID hash is required")
    @Size(max = 64)
    @Column(name = "external_id_hash", nullable = false, length = 64)
    private String externalIdHash;

    @Email(message = "Invalid email format")
    @Size(max = 255)
    @Column(name = "email")
    private String email;

    @Size(max = 64)
    @Column(name = "email_hash", length = 64)
    private String emailHash;

    @NotNull
    @Column(name = "eligible", nullable = false)
    @Builder.Default
    private Boolean eligible = true;

    @Size(max = 1000)
    @Column(name = "ineligibility_reason", length = 1000)
    private String ineligibilityReason;

    @Column(name = "registered_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant registeredAt = Instant.now();

    // Lifecycle callbacks

    @PrePersist
    @PreUpdate
    private void hashIdentifiers() {
        if (externalId != null && externalIdHash == null) {
            this.externalIdHash = hashSHA256(externalId);
        }
        if (email != null && emailHash == null) {
            this.emailHash = hashSHA256(email);
        }
    }

    // Business methods

    /**
     * Check if voter is eligible to vote
     */
    public boolean isEligible() {
        return Boolean.TRUE.equals(eligible);
    }

    /**
     * Mark voter as ineligible
     */
    public void markAsIneligible(String reason) {
        this.eligible = false;
        this.ineligibilityReason = reason;
    }

    /**
     * Mark voter as eligible
     */
    public void markAsEligible() {
        this.eligible = true;
        this.ineligibilityReason = null;
    }

    /**
     * Check if voter belongs to a specific election
     */
    public boolean belongsToElection(Election election) {
        return this.election != null && this.election.equals(election);
    }

    /**
     * Verify external ID matches
     */
    public boolean matchesExternalId(String externalId) {
        if (externalId == null || this.externalIdHash == null) {
            return false;
        }
        return this.externalIdHash.equals(hashSHA256(externalId));
    }

    /**
     * Verify email matches
     */
    public boolean matchesEmail(String email) {
        if (email == null || this.emailHash == null) {
            return false;
        }
        return this.emailHash.equals(hashSHA256(email));
    }

    // Utility methods

    /**
     * Hash a string using SHA-256
     */
    private static String hashSHA256(String input) {
        return DigestUtils.sha256Hex(input.toLowerCase().trim());
    }

    /**
     * Create hash for external ID (static method for queries)
     */
    public static String hashExternalId(String externalId) {
        return hashSHA256(externalId);
    }

    /**
     * Create hash for email (static method for queries)
     */
    public static String hashEmail(String email) {
        return hashSHA256(email);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Voter)) return false;
        Voter voter = (Voter) o;
        return id != null && id.equals(voter.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Voter{" +
                "id=" + id +
                ", externalIdHash='" + externalIdHash + '\'' +
                ", eligible=" + eligible +
                '}';
    }
}
