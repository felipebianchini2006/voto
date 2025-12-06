package com.votoeletronico.voto.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.Instant;

@Schema(description = "Request to create a new election")
public record CreateElectionRequest(

        @NotBlank(message = "Election name is required")
        @Size(min = 3, max = 255, message = "Name must be between 3 and 255 characters")
        @Schema(description = "Election name", example = "Presidential Election 2025")
        String name,

        @Size(max = 5000, message = "Description must not exceed 5000 characters")
        @Schema(description = "Detailed description of the election", example = "Annual election for company president")
        String description,

        @NotNull(message = "Start timestamp is required")
        @FutureOrPresent(message = "Start timestamp cannot be in the past")
        @Schema(description = "Election start date and time", example = "2025-12-15T08:00:00Z")
        Instant startTs,

        @NotNull(message = "End timestamp is required")
        @Schema(description = "Election end date and time", example = "2025-12-15T18:00:00Z")
        Instant endTs,

        @Positive(message = "Max votes per voter must be positive")
        @Schema(description = "Maximum number of votes each voter can cast", example = "1", defaultValue = "1")
        Integer maxVotesPerVoter,

        @Schema(description = "Allow voters to abstain", example = "true", defaultValue = "true")
        Boolean allowAbstention,

        @Schema(description = "Require justification for abstention", example = "false", defaultValue = "false")
        Boolean requireJustification

) {
    public CreateElectionRequest {
        // Set defaults
        if (maxVotesPerVoter == null) {
            maxVotesPerVoter = 1;
        }
        if (allowAbstention == null) {
            allowAbstention = true;
        }
        if (requireJustification == null) {
            requireJustification = false;
        }

        // Validate date range
        if (startTs != null && endTs != null && !endTs.isAfter(startTs)) {
            throw new IllegalArgumentException("End date must be after start date");
        }
    }
}
