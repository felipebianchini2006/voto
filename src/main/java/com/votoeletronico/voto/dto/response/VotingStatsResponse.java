package com.votoeletronico.voto.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * Voting statistics for an election
 */
@Schema(description = "Voting statistics")
public record VotingStatsResponse(
        @Schema(description = "Election ID")
        UUID electionId,

        @Schema(description = "Total tokens issued")
        long tokensIssued,

        @Schema(description = "Tokens consumed (votes cast)")
        long tokensConsumed,

        @Schema(description = "Tokens still valid")
        long tokensValid,

        @Schema(description = "Total ballots cast")
        long totalBallots,

        @Schema(description = "Ballots tallied")
        long ballotsTallied,

        @Schema(description = "Ballots pending tally")
        long ballotsPending,

        @Schema(description = "Voter turnout percentage")
        double turnoutPercentage
) {
    public VotingStatsResponse(UUID electionId, long tokensIssued, long tokensConsumed,
                                long tokensValid, long totalBallots, long ballotsTallied, long ballotsPending) {
        this(electionId, tokensIssued, tokensConsumed, tokensValid, totalBallots, ballotsTallied, ballotsPending,
                tokensIssued > 0 ? (tokensConsumed * 100.0 / tokensIssued) : 0.0);
    }
}
