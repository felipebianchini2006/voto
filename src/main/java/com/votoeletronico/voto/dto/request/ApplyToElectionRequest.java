package com.votoeletronico.voto.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for candidate to apply to an election
 */
public record ApplyToElectionRequest(
        @NotBlank(message = "Full name is required")
        @Size(min = 2, max = 255)
        String fullName,

        @Size(max = 5000)
        String description,

        @Size(max = 100)
        String party,

        @Size(max = 500)
        String photoUrl
) {}
