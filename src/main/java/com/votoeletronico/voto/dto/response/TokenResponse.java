package com.votoeletronico.voto.dto.response;

import com.votoeletronico.voto.domain.voting.TokenStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Response containing blind token information
 * NOTE: In production, the actual token value should be returned securely
 * and NOT stored in the database
 */
@Schema(description = "Blind token for anonymous voting")
public record TokenResponse(
        @Schema(description = "Token ID")
        UUID id,

        @Schema(description = "Election ID")
        UUID electionId,

        @Schema(description = "Token status")
        TokenStatus status,

        @Schema(description = "When token was issued")
        Instant issuedAt,

        @Schema(description = "When token expires")
        Instant expiresAt,

        @Schema(description = "Digital signature for verification")
        String signature,

        @Schema(description = "Nonce for replay prevention")
        String nonce,

        @Schema(description = "Public key for verification (Base64)")
        String publicKey,

        @Schema(description = "The actual token value - STORE THIS SECURELY!")
        String tokenValue
) {
}
