package com.votoeletronico.voto.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Result of voter import operation")
public record VoterImportResult(

        @Schema(description = "Total voters processed", example = "100")
        int totalProcessed,

        @Schema(description = "Successfully imported voters", example = "95")
        int successCount,

        @Schema(description = "Failed imports", example = "5")
        int failureCount,

        @Schema(description = "List of import errors")
        List<ImportError> errors

) {
    @Schema(description = "Import error details")
    public record ImportError(
            @Schema(description = "Line number in CSV", example = "42")
            int lineNumber,

            @Schema(description = "External ID from CSV", example = "12345")
            String externalId,

            @Schema(description = "Error message")
            String errorMessage
    ) {}
}
