package com.votoeletronico.voto.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating candidate profile
 */
public record UpdateCandidateProfileRequest(
        @Size(min = 2, max = 255)
        String fullName,

        @Size(max = 500)
        String photoUrl,

        @Size(max = 5000)
        String description,

        @Size(max = 100)
        String party
) {}
