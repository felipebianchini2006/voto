package com.votoeletronico.voto.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Voter statistics for an election")
public record VoterStatsResponse(

        @Schema(description = "Election unique identifier")
        UUID electionId,

        @Schema(description = "Total number of registered voters", example = "1500")
        Long totalVoters,

        @Schema(description = "Number of eligible voters", example = "1450")
        Long eligibleVoters,

        @Schema(description = "Number of ineligible voters", example = "50")
        Long ineligibleVoters,

        @Schema(description = "Percentage of eligible voters", example = "96.67")
        Double eligibilityPercentage

) {
    public VoterStatsResponse(UUID electionId, Long totalVoters, Long eligibleVoters, Long ineligibleVoters) {
        this(
                electionId,
                totalVoters,
                eligibleVoters,
                ineligibleVoters,
                totalVoters > 0 ? (eligibleVoters * 100.0 / totalVoters) : 0.0
        );
    }
}
