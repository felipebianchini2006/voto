package com.votoeletronico.voto.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to issue a blind token for voting
 */
@Schema(description = "Request to obtain a voting token")
public record TokenRequest(
        @Schema(description = "Voter's external ID (CPF)", example = "12345678900")
        @NotBlank(message = "External ID is required")
        @Size(min = 11, max = 11, message = "External ID must be 11 characters")
        String externalId
) {
}
