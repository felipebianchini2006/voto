package com.votoeletronico.voto.domain.results;

import com.votoeletronico.voto.domain.BaseEntity;
import com.votoeletronico.voto.domain.election.Election;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Final results of an election after tally
 * Immutable once published
 */
@Entity
@Table(name = "election_results", indexes = {
        @Index(name = "idx_election_results_election", columnList = "election_id", unique = true),
        @Index(name = "idx_election_results_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ElectionResult extends BaseEntity {

    /**
     * The election these results belong to
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "election_id", nullable = false, unique = true)
    @NotNull
    private Election election;

    /**
     * Status of the tally process
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @NotNull
    @Builder.Default
    private TallyStatus status = TallyStatus.PENDING;

    /**
     * When tally was started
     */
    @Column(name = "tally_started_at")
    private Instant tallyStartedAt;

    /**
     * When tally was completed
     */
    @Column(name = "tally_completed_at")
    private Instant tallyCompletedAt;

    /**
     * Total ballots processed
     */
    @Column(name = "total_ballots", nullable = false)
    @Builder.Default
    private Long totalBallots = 0L;

    /**
     * Valid votes (for candidates)
     */
    @Column(name = "valid_votes", nullable = false)
    @Builder.Default
    private Long validVotes = 0L;

    /**
     * Abstentions
     */
    @Column(name = "abstentions", nullable = false)
    @Builder.Default
    private Long abstentions = 0L;

    /**
     * Invalid votes (couldn't be decrypted or processed)
     */
    @Column(name = "invalid_votes", nullable = false)
    @Builder.Default
    private Long invalidVotes = 0L;

    /**
     * Tokens issued
     */
    @Column(name = "tokens_issued", nullable = false)
    @Builder.Default
    private Long tokensIssued = 0L;

    /**
     * Voter turnout percentage
     */
    @Column(name = "turnout_percentage", nullable = false)
    @Builder.Default
    private Double turnoutPercentage = 0.0;

    /**
     * Merkle root of all ballots (for verification)
     */
    @Column(name = "merkle_root", length = 64)
    private String merkleRoot;

    /**
     * Hash of the final results (for integrity)
     */
    @Column(name = "results_hash", length = 64)
    private String resultsHash;

    /**
     * Digital signature of results
     */
    @Column(name = "results_signature", columnDefinition = "TEXT")
    private String resultsSignature;

    /**
     * User who initiated the tally
     */
    @Column(name = "tallied_by")
    private UUID talliedBy;

    /**
     * Published (results made public)
     */
    @Column(name = "published", nullable = false)
    @Builder.Default
    private Boolean published = false;

    /**
     * When results were published
     */
    @Column(name = "published_at")
    private Instant publishedAt;

    /**
     * Results per candidate
     */
    @OneToMany(mappedBy = "electionResult", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CandidateResult> candidateResults = new ArrayList<>();

    /**
     * Notes or observations about the tally
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // Business methods

    /**
     * Start tally process
     */
    public void startTally(UUID userId) {
        if (status != TallyStatus.PENDING) {
            throw new IllegalStateException("Tally already started or completed");
        }
        this.status = TallyStatus.IN_PROGRESS;
        this.tallyStartedAt = Instant.now();
        this.talliedBy = userId;
    }

    /**
     * Complete tally process
     */
    public void completeTally(String merkleRoot, String resultsHash, String signature) {
        if (status != TallyStatus.IN_PROGRESS) {
            throw new IllegalStateException("Tally not in progress");
        }
        this.status = TallyStatus.COMPLETED;
        this.tallyCompletedAt = Instant.now();
        this.merkleRoot = merkleRoot;
        this.resultsHash = resultsHash;
        this.resultsSignature = signature;
    }

    /**
     * Mark tally as failed
     */
    public void failTally(String reason) {
        this.status = TallyStatus.FAILED;
        this.notes = reason;
    }

    /**
     * Publish results
     */
    public void publish() {
        if (status != TallyStatus.COMPLETED) {
            throw new IllegalStateException("Cannot publish incomplete tally");
        }
        this.published = true;
        this.publishedAt = Instant.now();
    }

    /**
     * Add candidate result
     */
    public void addCandidateResult(CandidateResult result) {
        result.setElectionResult(this);
        this.candidateResults.add(result);
    }

    /**
     * Calculate turnout
     */
    public void calculateTurnout() {
        if (tokensIssued > 0) {
            this.turnoutPercentage = (totalBallots * 100.0) / tokensIssued;
        }
    }

    /**
     * Check if results are final
     */
    public boolean isFinal() {
        return status == TallyStatus.COMPLETED || status == TallyStatus.VERIFIED;
    }
}
