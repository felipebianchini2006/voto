package com.votoeletronico.voto.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Voter information response (anonymized)")
public record VoterResponse(

        @Schema(description = "Voter unique identifier")
        UUID id,

        @Schema(description = "Election ID")
        UUID electionId,

        @Schema(description = "External ID (masked for privacy)", example = "****5678")
        String externalIdMasked,

        @Schema(description = "Email (masked for privacy)", example = "v****@example.com")
        String emailMasked,

        @Schema(description = "Whether voter is eligible")
        Boolean eligible,

        @Schema(description = "Reason for ineligibility (if applicable)")
        String ineligibilityReason,

        @Schema(description = "Registration timestamp")
        Instant registeredAt

) {
}
