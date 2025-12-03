package com.votoeletronico.voto.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request to cast a vote
 */
@Schema(description = "Request to cast a vote for a candidate")
public record CastVoteRequest(
        @Schema(description = "Blind token issued to the voter", example = "abc123...")
        @NotBlank(message = "Token is required")
        String token,

        @Schema(description = "ID of the candidate to vote for")
        @NotNull(message = "Candidate ID is required")
        UUID candidateId
) {
}
