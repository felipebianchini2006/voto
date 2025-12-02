package com.votoeletronico.voto.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to create a new candidate")
public record CreateCandidateRequest(

        @NotBlank(message = "Candidate name is required")
        @Size(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
        @Schema(description = "Candidate full name", example = "John Doe")
        String name,

        @Positive(message = "Ballot number must be positive")
        @Schema(description = "Ballot number for this candidate", example = "42")
        Integer ballotNumber,

        @Size(max = 5000, message = "Description must not exceed 5000 characters")
        @Schema(description = "Candidate description or biography", example = "Experienced leader with 10 years in public service")
        String description,

        @Size(max = 500, message = "Photo URL must not exceed 500 characters")
        @Schema(description = "URL to candidate photo", example = "https://example.com/photos/john-doe.jpg")
        String photoUrl

) {
}
