package com.votoeletronico.voto.domain.election;

import com.votoeletronico.voto.domain.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Election entity representing an electoral process
 */
@Entity
@Table(name = "elections", indexes = {
        @Index(name = "idx_elections_status", columnList = "status"),
        @Index(name = "idx_elections_dates", columnList = "start_ts, end_ts"),
        @Index(name = "idx_elections_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Election extends BaseEntity {

    @NotBlank(message = "Election name is required")
    @Size(min = 3, max = 255, message = "Election name must be between 3 and 255 characters")
    @Column(name = "name", nullable = false)
    private String name;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Start timestamp is required")
    @Column(name = "start_ts", nullable = false)
    private Instant startTs;

    @NotNull(message = "End timestamp is required")
    @Column(name = "end_ts", nullable = false)
    private Instant endTs;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private ElectionStatus status = ElectionStatus.DRAFT;

    @Positive(message = "Max votes per voter must be positive")
    @Column(name = "max_votes_per_voter", nullable = false)
    @Builder.Default
    private Integer maxVotesPerVoter = 1;

    @Column(name = "allow_abstention", nullable = false)
    @Builder.Default
    private Boolean allowAbstention = true;

    @Column(name = "require_justification", nullable = false)
    @Builder.Default
    private Boolean requireJustification = false;

    @Column(name = "created_by")
    private UUID createdBy;

    @OneToMany(mappedBy = "election", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Candidate> candidates = new ArrayList<>();

    // Business methods

    /**
     * Check if election is in DRAFT status
     */
    public boolean isDraft() {
        return this.status == ElectionStatus.DRAFT;
    }

    /**
     * Check if election is currently running
     */
    public boolean isRunning() {
        return this.status == ElectionStatus.RUNNING;
    }

    /**
     * Check if election is closed
     */
    public boolean isClosed() {
        return this.status == ElectionStatus.CLOSED;
    }

    /**
     * Check if election is cancelled
     */
    public boolean isCancelled() {
        return this.status == ElectionStatus.CANCELLED;
    }

    /**
     * Check if election can be modified
     * Only DRAFT elections can be modified
     */
    public boolean canBeModified() {
        return isDraft();
    }

    /**
     * Check if election can be started
     */
    public boolean canBeStarted() {
        Instant now = Instant.now();
        return isDraft()
                && !candidates.isEmpty()
                && startTs.isBefore(endTs)
                && now.isBefore(endTs);
    }

    /**
     * Check if election can be closed
     */
    public boolean canBeClosed() {
        return isRunning();
    }

    /**
     * Check if voting is currently open
     */
    public boolean isVotingOpen() {
        if (!isRunning()) {
            return false;
        }
        Instant now = Instant.now();
        return !now.isBefore(startTs) && now.isBefore(endTs);
    }

    /**
     * Add a candidate to this election
     */
    public void addCandidate(Candidate candidate) {
        candidates.add(candidate);
        candidate.setElection(this);
    }

    /**
     * Remove a candidate from this election
     */
    public void removeCandidate(Candidate candidate) {
        candidates.remove(candidate);
        candidate.setElection(null);
    }

    /**
     * Validate election dates
     */
    @AssertTrue(message = "End date must be after start date")
    private boolean isValidDateRange() {
        if (startTs == null || endTs == null) {
            return true; // Let @NotNull handle null validation
        }
        return endTs.isAfter(startTs);
    }

    /**
     * Validate max votes per voter
     */
    @AssertTrue(message = "Max votes per voter must not exceed number of candidates")
    private boolean isValidMaxVotes() {
        if (maxVotesPerVoter == null || candidates.isEmpty()) {
            return true;
        }
        return maxVotesPerVoter <= candidates.size();
    }
}
