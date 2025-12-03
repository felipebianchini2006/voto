package com.votoeletronico.voto.domain.results;

import com.votoeletronico.voto.domain.BaseEntity;
import com.votoeletronico.voto.domain.election.Candidate;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Vote count for a specific candidate
 */
@Entity
@Table(name = "candidate_results", indexes = {
        @Index(name = "idx_candidate_results_election_result", columnList = "election_result_id"),
        @Index(name = "idx_candidate_results_candidate", columnList = "candidate_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateResult extends BaseEntity {

    /**
     * The election result this belongs to
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "election_result_id", nullable = false)
    @NotNull
    private ElectionResult electionResult;

    /**
     * The candidate
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "candidate_id", nullable = false)
    @NotNull
    private Candidate candidate;

    /**
     * Number of votes received
     */
    @Column(name = "vote_count", nullable = false)
    @Builder.Default
    private Long voteCount = 0L;

    /**
     * Percentage of valid votes
     */
    @Column(name = "percentage", nullable = false)
    @Builder.Default
    private Double percentage = 0.0;

    /**
     * Position in ranking (1st, 2nd, 3rd, etc.)
     */
    @Column(name = "rank_position")
    private Integer rankPosition;

    /**
     * Whether this candidate won
     */
    @Column(name = "is_winner", nullable = false)
    @Builder.Default
    private Boolean isWinner = false;

    // Business methods

    /**
     * Calculate percentage of total votes
     */
    public void calculatePercentage(long totalValidVotes) {
        if (totalValidVotes > 0) {
            this.percentage = (voteCount * 100.0) / totalValidVotes;
        }
    }

    /**
     * Increment vote count
     */
    public void incrementVotes() {
        this.voteCount++;
    }

    /**
     * Set as winner
     */
    public void markAsWinner() {
        this.isWinner = true;
        this.rankPosition = 1;
    }
}
