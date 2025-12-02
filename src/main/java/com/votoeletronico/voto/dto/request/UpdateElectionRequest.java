package com.votoeletronico.voto.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@Schema(description = "Request to update an existing election (only DRAFT elections can be updated)")
public record UpdateElectionRequest(

        @Size(min = 3, max = 255, message = "Name must be between 3 and 255 characters")
        @Schema(description = "Election name", example = "Presidential Election 2025")
        String name,

        @Size(max = 5000, message = "Description must not exceed 5000 characters")
        @Schema(description = "Detailed description of the election")
        String description,

        @Schema(description = "Election start date and time", example = "2025-12-15T08:00:00Z")
        Instant startTs,

        @Schema(description = "Election end date and time", example = "2025-12-15T18:00:00Z")
        Instant endTs,

        @Positive(message = "Max votes per voter must be positive")
        @Schema(description = "Maximum number of votes each voter can cast", example = "1")
        Integer maxVotesPerVoter,

        @Schema(description = "Allow voters to abstain", example = "true")
        Boolean allowAbstention,

        @Schema(description = "Require justification for abstention", example = "false")
        Boolean requireJustification

) {
}
