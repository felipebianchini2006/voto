package com.votoeletronico.voto.domain.election;

import com.votoeletronico.voto.domain.BaseEntity;
import com.votoeletronico.voto.domain.user.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.UUID;

/**
 * Candidate entity representing a voting option in an election.
 *
 * Candidates can be created in two ways:
 * 1. Admin-created: No user association (user_id = null)
 * 2. Self-registered: Linked to a User account (user_id set)
 */
@Entity
@Table(name = "candidates",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_candidate_ballot_number",
                        columnNames = {"election_id", "ballot_number"})
        },
        indexes = {
                @Index(name = "idx_candidates_election", columnList = "election_id"),
                @Index(name = "idx_candidates_ballot_number", columnList = "election_id, ballot_number")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Candidate extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "election_id", nullable = false, foreignKey = @ForeignKey(name = "fk_candidate_election"))
    @JsonIgnore
    private Election election;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_candidate_user"))
    @JsonIgnore
    private User user;

    @NotBlank(message = "Candidate name is required")
    @Size(min = 2, max = 255, message = "Candidate name must be between 2 and 255 characters")
    @Column(name = "name", nullable = false)
    private String name;

    @NotNull(message = "Ballot number is required")
    @Positive(message = "Ballot number must be positive")
    @Column(name = "ballot_number", nullable = false)
    private Integer ballotNumber;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Size(max = 500, message = "Photo URL must not exceed 500 characters")
    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Size(max = 100, message = "Party must not exceed 100 characters")
    @Column(name = "party", length = 100)
    private String party;

    // Business methods

    /**
     * Check if this candidate belongs to a specific election
     */
    public boolean belongsToElection(Election election) {
        return this.election != null && this.election.equals(election);
    }

    /**
     * Check if candidate can be modified
     * Only candidates in DRAFT elections can be modified
     */
    public boolean canBeModified() {
        return election != null && election.canBeModified();
    }

    /**
     * Check if this candidate has a user account (self-registered)
     */
    public boolean hasUserAccount() {
        return user != null;
    }

    /**
     * Check if this candidate belongs to a specific user
     */
    public boolean belongsToUser(UUID userId) {
        return user != null && user.getId() != null && user.getId().equals(userId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Candidate)) return false;
        Candidate candidate = (Candidate) o;
        return getId() != null && getId().equals(candidate.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Candidate{" +
                "id=" + getId() +
                ", name='" + name + '\'' +
                ", ballotNumber=" + ballotNumber +
                '}';
    }
}
