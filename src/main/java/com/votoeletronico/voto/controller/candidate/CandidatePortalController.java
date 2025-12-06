package com.votoeletronico.voto.controller.candidate;

import com.votoeletronico.voto.domain.user.User;
import com.votoeletronico.voto.dto.request.ApplyToElectionRequest;
import com.votoeletronico.voto.dto.request.UpdateCandidateProfileRequest;
import com.votoeletronico.voto.dto.response.CandidateResponse;
import com.votoeletronico.voto.dto.response.ElectionResponse;
import com.votoeletronico.voto.service.AuthenticationService;
import com.votoeletronico.voto.service.CandidateService;
import com.votoeletronico.voto.service.ElectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Candidate Portal Controller
 * Endpoints for candidates to manage their elections
 */
@Tag(name = "Candidate Portal", description = "Endpoints for candidates to manage their elections")
@RestController
@RequestMapping("/api/candidate")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
public class CandidatePortalController {

    private final CandidateService candidateService;
    private final ElectionService electionService;
    private final AuthenticationService authenticationService;

    @Operation(summary = "Get my candidacies", description = "Get all elections I'm registered for")
    @GetMapping("/elections")
    public ResponseEntity<List<CandidateResponse>> getMyElections(Authentication authentication) {
        User user = authenticationService.getCurrentUser(authentication);
        List<CandidateResponse> candidates = candidateService.getCandidatesForUser(user.getId());
        return ResponseEntity.ok(candidates);
    }

    @Operation(summary = "Get available elections", description = "Get elections available to apply")
    @GetMapping("/elections/available")
    public ResponseEntity<List<ElectionResponse>> getAvailableElections() {
        // Return all elections without pagination for candidate portal
        List<ElectionResponse> elections = electionService.getAllElections(PageRequest.of(0, 100))
                .getContent();
        return ResponseEntity.ok(elections);
    }

    @Operation(summary = "Apply to election", description = "Register as candidate for an election")
    @PostMapping("/elections/{electionId}/apply")
    public ResponseEntity<CandidateResponse> applyToElection(
            @PathVariable UUID electionId,
            @Valid @RequestBody ApplyToElectionRequest request,
            Authentication authentication) {
        User user = authenticationService.getCurrentUser(authentication);
        CandidateResponse response = candidateService.applyToElection(user.getId(), electionId, request);
        return ResponseEntity.status(201).body(response);
    }

    @Operation(summary = "Withdraw from election", description = "Remove candidacy from election")
    @DeleteMapping("/elections/{electionId}/withdraw")
    public ResponseEntity<Void> withdrawFromElection(
            @PathVariable UUID electionId,
            Authentication authentication) {
        User user = authenticationService.getCurrentUser(authentication);
        candidateService.withdrawFromElection(user.getId(), electionId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Update profile", description = "Update candidate profile information")
    @PutMapping("/candidates/{candidateId}/profile")
    public ResponseEntity<CandidateResponse> updateProfile(
            @PathVariable UUID candidateId,
            @Valid @RequestBody UpdateCandidateProfileRequest request,
            Authentication authentication) {
        User user = authenticationService.getCurrentUser(authentication);
        CandidateResponse response = candidateService.updateCandidateProfile(
                user.getId(), candidateId, request);
        return ResponseEntity.ok(response);
    }
}
