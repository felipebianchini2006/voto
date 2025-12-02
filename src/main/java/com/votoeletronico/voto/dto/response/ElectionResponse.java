package com.votoeletronico.voto.dto.response;

import com.votoeletronico.voto.domain.election.ElectionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Election details response")
public record ElectionResponse(

        @Schema(description = "Election unique identifier")
        UUID id,

        @Schema(description = "Election name", example = "Presidential Election 2025")
        String name,

        @Schema(description = "Election description")
        String description,

        @Schema(description = "Election start date and time")
        Instant startTs,

        @Schema(description = "Election end date and time")
        Instant endTs,

        @Schema(description = "Current election status")
        ElectionStatus status,

        @Schema(description = "Maximum votes per voter", example = "1")
        Integer maxVotesPerVoter,

        @Schema(description = "Whether abstention is allowed")
        Boolean allowAbstention,

        @Schema(description = "Whether justification is required for abstention")
        Boolean requireJustification,

        @Schema(description = "User who created this election")
        UUID createdBy,

        @Schema(description = "Election creation timestamp")
        Instant createdAt,

        @Schema(description = "Last update timestamp")
        Instant updatedAt,

        @Schema(description = "List of candidates (if requested)")
        List<CandidateResponse> candidates,

        @Schema(description = "Whether voting is currently open")
        Boolean votingOpen

) {
}
