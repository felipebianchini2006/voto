package com.votoeletronico.voto.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Response for ballot verification
 */
@Schema(description = "Ballot verification result")
public record BallotVerificationResponse(
        @Schema(description = "Whether ballot exists")
        boolean exists,

        @Schema(description = "Whether ballot has been tallied")
        boolean tallied,

        @Schema(description = "When ballot was cast")
        Instant castAt,

        @Schema(description = "Verification message")
        String message
) {
    public static BallotVerificationResponse found(Instant castAt, boolean tallied) {
        return new BallotVerificationResponse(
                true,
                tallied,
                castAt,
                tallied ? "Your vote has been counted in the tally."
                        : "Your vote has been recorded and will be counted."
        );
    }

    public static BallotVerificationResponse notFound() {
        return new BallotVerificationResponse(
                false,
                false,
                null,
                "Ballot not found. Please check your ballot hash."
        );
    }
}
