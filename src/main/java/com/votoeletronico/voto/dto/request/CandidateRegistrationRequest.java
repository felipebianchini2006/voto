package com.votoeletronico.voto.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Candidate self-registration request DTO
 */
public record CandidateRegistrationRequest(
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 100)
        String username,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        @NotBlank(message = "Full name is required")
        @Size(min = 2, max = 255)
        String fullName,

        @NotBlank(message = "Email is required")
        @Email
        String email,

        @Size(max = 5000)
        String description,

        @Size(max = 100)
        String party
) {}
