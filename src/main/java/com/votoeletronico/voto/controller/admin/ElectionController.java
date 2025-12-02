package com.votoeletronico.voto.controller.admin;

import com.votoeletronico.voto.domain.election.ElectionStatus;
import com.votoeletronico.voto.dto.request.CreateElectionRequest;
import com.votoeletronico.voto.dto.request.UpdateElectionRequest;
import com.votoeletronico.voto.dto.response.ElectionResponse;
import com.votoeletronico.voto.service.ElectionService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Election Management", description = "Admin endpoints for managing elections")
@RestController
@RequestMapping("/api/admin/elections")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
public class ElectionController {

    private final ElectionService electionService;

    @Operation(summary = "Create a new election", description = "Creates a new election in DRAFT status")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Election created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<ElectionResponse> createElection(
            @Valid @RequestBody CreateElectionRequest request,
            @Parameter(hidden = true) @RequestAttribute(value = "userId", required = false) UUID userId) {

        ElectionResponse response = electionService.createElection(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get election by ID", description = "Retrieve detailed information about a specific election")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Election found"),
            @ApiResponse(responseCode = "404", description = "Election not found")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'AUDITOR')")
    public ResponseEntity<ElectionResponse> getElectionById(@PathVariable UUID id) {
        ElectionResponse response = electionService.getElectionWithCandidates(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get all elections", description = "Retrieve all elections with pagination")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'AUDITOR')")
    public ResponseEntity<Page<ElectionResponse>> getAllElections(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        Page<ElectionResponse> response = electionService.getAllElections(pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get elections by status", description = "Filter elections by status with pagination")
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'AUDITOR')")
    public ResponseEntity<Page<ElectionResponse>> getElectionsByStatus(
            @PathVariable ElectionStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ElectionResponse> response = electionService.getElectionsByStatus(status, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Search elections by name", description = "Search elections by name (case-insensitive)")
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'AUDITOR')")
    public ResponseEntity<Page<ElectionResponse>> searchElections(
            @RequestParam String name,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ElectionResponse> response = electionService.searchElections(name, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get active elections", description = "Get all currently running elections")
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'AUDITOR')")
    public ResponseEntity<List<ElectionResponse>> getActiveElections() {
        List<ElectionResponse> response = electionService.getActiveElections();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get upcoming elections", description = "Get elections that haven't started yet")
    @GetMapping("/upcoming")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'AUDITOR')")
    public ResponseEntity<List<ElectionResponse>> getUpcomingElections() {
        List<ElectionResponse> response = electionService.getUpcomingElections();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get past elections", description = "Get elections that have ended")
    @GetMapping("/past")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'AUDITOR')")
    public ResponseEntity<List<ElectionResponse>> getPastElections() {
        List<ElectionResponse> response = electionService.getPastElections();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update election", description = "Update an election (only DRAFT elections can be updated)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Election updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or election cannot be modified"),
            @ApiResponse(responseCode = "404", description = "Election not found")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<ElectionResponse> updateElection(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateElectionRequest request) {
        ElectionResponse response = electionService.updateElection(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Start election", description = "Change election status from DRAFT to RUNNING")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Election started successfully"),
            @ApiResponse(responseCode = "400", description = "Election cannot be started"),
            @ApiResponse(responseCode = "404", description = "Election not found")
    })
    @PostMapping("/{id}/start")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ElectionResponse> startElection(@PathVariable UUID id) {
        ElectionResponse response = electionService.startElection(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Close election", description = "Change election status from RUNNING to CLOSED")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Election closed successfully"),
            @ApiResponse(responseCode = "400", description = "Election cannot be closed"),
            @ApiResponse(responseCode = "404", description = "Election not found")
    })
    @PostMapping("/{id}/close")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ElectionResponse> closeElection(@PathVariable UUID id) {
        ElectionResponse response = electionService.closeElection(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Cancel election", description = "Cancel an election")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Election cancelled successfully"),
            @ApiResponse(responseCode = "400", description = "Election cannot be cancelled"),
            @ApiResponse(responseCode = "404", description = "Election not found")
    })
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ElectionResponse> cancelElection(@PathVariable UUID id) {
        ElectionResponse response = electionService.cancelElection(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete election", description = "Delete an election (only DRAFT elections can be deleted)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Election deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Election cannot be deleted"),
            @ApiResponse(responseCode = "404", description = "Election not found")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteElection(@PathVariable UUID id) {
        electionService.deleteElection(id);
        return ResponseEntity.noContent().build();
    }
}
