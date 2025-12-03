package com.votoeletronico.voto.controller.admin;

import com.votoeletronico.voto.dto.request.RegisterVoterRequest;
import com.votoeletronico.voto.dto.response.VoterImportResult;
import com.votoeletronico.voto.dto.response.VoterResponse;
import com.votoeletronico.voto.dto.response.VoterStatsResponse;
import com.votoeletronico.voto.service.VoterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Tag(name = "Voter Management", description = "Admin endpoints for managing voters")
@RestController
@RequestMapping("/api/admin/elections/{electionId}/voters")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
public class VoterController {

    private final VoterService voterService;

    @Operation(summary = "Register a single voter", description = "Register a voter for an election")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Voter registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or voter already registered"),
            @ApiResponse(responseCode = "404", description = "Election not found")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<VoterResponse> registerVoter(
            @PathVariable UUID electionId,
            @Valid @RequestBody RegisterVoterRequest request) {
        VoterResponse response = voterService.registerVoter(electionId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get voters for election", description = "Retrieve all voters for a specific election (paginated)")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'AUDITOR')")
    public ResponseEntity<Page<VoterResponse>> getVoters(
            @PathVariable UUID electionId,
            @PageableDefault(size = 50, sort = "registeredAt") Pageable pageable) {
        Page<VoterResponse> response = voterService.getVotersByElection(electionId, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get voter by ID", description = "Retrieve details of a specific voter")
    @GetMapping("/{voterId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'AUDITOR')")
    public ResponseEntity<VoterResponse> getVoterById(
            @PathVariable UUID electionId,
            @PathVariable UUID voterId) {
        VoterResponse response = voterService.getVoterById(voterId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update voter eligibility", description = "Mark voter as eligible or ineligible")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Voter eligibility updated"),
            @ApiResponse(responseCode = "400", description = "Election is not in DRAFT status"),
            @ApiResponse(responseCode = "404", description = "Voter not found")
    })
    @PatchMapping("/{voterId}/eligibility")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<VoterResponse> updateEligibility(
            @PathVariable UUID electionId,
            @PathVariable UUID voterId,
            @RequestParam boolean eligible,
            @RequestParam(required = false) String reason) {
        VoterResponse response = voterService.updateVoterEligibility(voterId, eligible, reason);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete voter", description = "Remove a voter from the election (only for DRAFT elections)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Voter deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Election is not in DRAFT status"),
            @ApiResponse(responseCode = "404", description = "Voter not found")
    })
    @DeleteMapping("/{voterId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Void> deleteVoter(
            @PathVariable UUID electionId,
            @PathVariable UUID voterId) {
        voterService.deleteVoter(voterId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get voter statistics", description = "Get statistics about voters for this election")
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'AUDITOR')")
    public ResponseEntity<VoterStatsResponse> getVoterStatistics(@PathVariable UUID electionId) {
        VoterStatsResponse response = voterService.getVoterStatistics(electionId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Import voters from CSV",
            description = """
                    Import multiple voters from a CSV file.

                    CSV Format:
                    - First line (header): externalId,email,eligible
                    - Following lines: data rows

                    Example:
                    externalId,email,eligible
                    12345678900,voter1@example.com,true
                    98765432100,voter2@example.com,true
                    55544433322,voter3@example.com,false
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Import completed (check result for details)"),
            @ApiResponse(responseCode = "400", description = "Invalid file or election not in DRAFT status"),
            @ApiResponse(responseCode = "404", description = "Election not found")
    })
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<VoterImportResult> importVoters(
            @PathVariable UUID electionId,
            @Parameter(description = "CSV file with voter data")
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        if (!file.getOriginalFilename().toLowerCase().endsWith(".csv")) {
            throw new IllegalArgumentException("File must be a CSV file");
        }

        VoterImportResult result = voterService.importVotersFromCsv(electionId, file);
        return ResponseEntity.ok(result);
    }
}
