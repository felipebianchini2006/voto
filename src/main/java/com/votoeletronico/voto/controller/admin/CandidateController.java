package com.votoeletronico.voto.controller.admin;

import com.votoeletronico.voto.dto.request.CreateCandidateRequest;
import com.votoeletronico.voto.dto.response.CandidateResponse;
import com.votoeletronico.voto.service.CandidateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Candidate Management", description = "Admin endpoints for managing election candidates")
@RestController
@RequestMapping("/api/admin/elections/{electionId}/candidates")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
public class CandidateController {

    private final CandidateService candidateService;

    @Operation(summary = "Add candidate to election", description = "Create a new candidate for an election")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Candidate created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or election is not in DRAFT status"),
            @ApiResponse(responseCode = "404", description = "Election not found")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CandidateResponse> addCandidate(
            @PathVariable UUID electionId,
            @Valid @RequestBody CreateCandidateRequest request) {
        CandidateResponse response = candidateService.addCandidate(electionId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get all candidates for election", description = "Retrieve all candidates for a specific election")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'AUDITOR')")
    public ResponseEntity<List<CandidateResponse>> getCandidates(@PathVariable UUID electionId) {
        List<CandidateResponse> response = candidateService.getCandidatesByElection(electionId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get candidate by ID", description = "Retrieve details of a specific candidate")
    @GetMapping("/{candidateId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'AUDITOR')")
    public ResponseEntity<CandidateResponse> getCandidateById(
            @PathVariable UUID electionId,
            @PathVariable UUID candidateId) {
        CandidateResponse response = candidateService.getCandidateById(candidateId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update candidate", description = "Update a candidate (only for DRAFT elections)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Candidate updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or election is not in DRAFT status"),
            @ApiResponse(responseCode = "404", description = "Candidate not found")
    })
    @PutMapping("/{candidateId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CandidateResponse> updateCandidate(
            @PathVariable UUID electionId,
            @PathVariable UUID candidateId,
            @Valid @RequestBody CreateCandidateRequest request) {
        CandidateResponse response = candidateService.updateCandidate(candidateId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete candidate", description = "Delete a candidate (only for DRAFT elections)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Candidate deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Election is not in DRAFT status"),
            @ApiResponse(responseCode = "404", description = "Candidate not found")
    })
    @DeleteMapping("/{candidateId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Void> deleteCandidate(
            @PathVariable UUID electionId,
            @PathVariable UUID candidateId) {
        candidateService.deleteCandidate(candidateId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete all candidates", description = "Delete all candidates for an election (only for DRAFT elections)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "All candidates deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Election is not in DRAFT status"),
            @ApiResponse(responseCode = "404", description = "Election not found")
    })
    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAllCandidates(@PathVariable UUID electionId) {
        candidateService.deleteAllCandidatesForElection(electionId);
        return ResponseEntity.noContent().build();
    }
}
