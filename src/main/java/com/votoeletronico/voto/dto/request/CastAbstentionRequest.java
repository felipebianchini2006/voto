package com.votoeletronico.voto.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to cast an abstention
 */
@Schema(description = "Request to cast an abstention vote")
public record CastAbstentionRequest(
        @Schema(description = "Blind token issued to the voter", example = "abc123...")
        @NotBlank(message = "Token is required")
        String token,

        @Schema(description = "Justification for abstention (required if election requires it)")
        @Size(max = 500, message = "Justification must not exceed 500 characters")
        String justification
) {
}
