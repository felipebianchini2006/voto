package com.votoeletronico.voto.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Receipt for a cast vote
 * Voters can use the ballotHash to verify their vote was counted
 */
@Schema(description = "Receipt proving vote was cast")
public record VoteReceiptResponse(
        @Schema(description = "Ballot ID")
        UUID ballotId,

        @Schema(description = "Ballot hash - save this to verify your vote was counted!")
        String ballotHash,

        @Schema(description = "When vote was cast")
        Instant castAt,

        @Schema(description = "Verification signature")
        String verificationSignature,

        @Schema(description = "Message to voter")
        String message
) {
    public VoteReceiptResponse(UUID ballotId, String ballotHash, Instant castAt, String verificationSignature) {
        this(ballotId, ballotHash, castAt, verificationSignature,
                "Your vote has been recorded. Save your ballot hash to verify it was counted in the final tally.");
    }
}
