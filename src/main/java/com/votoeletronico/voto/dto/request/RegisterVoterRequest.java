package com.votoeletronico.voto.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to register a single voter")
public record RegisterVoterRequest(

        @NotBlank(message = "External ID is required")
        @Size(max = 255, message = "External ID must not exceed 255 characters")
        @Schema(description = "External identifier (e.g., employee ID, CPF)", example = "12345678900")
        String externalId,

        @Email(message = "Invalid email format")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        @Schema(description = "Voter email address", example = "voter@example.com")
        String email,

        @Schema(description = "Whether the voter is eligible to vote", example = "true", defaultValue = "true")
        Boolean eligible,

        @Size(max = 1000, message = "Ineligibility reason must not exceed 1000 characters")
        @Schema(description = "Reason for ineligibility (if applicable)")
        String ineligibilityReason

) {
    public RegisterVoterRequest {
        if (eligible == null) {
            eligible = true;
        }
    }
}
