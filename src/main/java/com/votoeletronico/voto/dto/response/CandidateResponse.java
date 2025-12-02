package com.votoeletronico.voto.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Candidate details response")
public record CandidateResponse(

        @Schema(description = "Candidate unique identifier")
        UUID id,

        @Schema(description = "Election ID this candidate belongs to")
        UUID electionId,

        @Schema(description = "Candidate full name", example = "John Doe")
        String name,

        @Schema(description = "Ballot number", example = "42")
        Integer ballotNumber,

        @Schema(description = "Candidate description or biography")
        String description,

        @Schema(description = "URL to candidate photo")
        String photoUrl,

        @Schema(description = "Candidate creation timestamp")
        Instant createdAt,

        @Schema(description = "Last update timestamp")
        Instant updatedAt

) {
}
