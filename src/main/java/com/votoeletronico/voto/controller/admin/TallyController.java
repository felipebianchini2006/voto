package com.votoeletronico.voto.controller.admin;

import com.votoeletronico.voto.domain.results.ElectionResult;
import com.votoeletronico.voto.dto.response.ElectionResultResponse;
import com.votoeletronico.voto.service.TallyService;
import com.votoeletronico.voto.service.mapper.ElectionResultMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/elections")
@RequiredArgsConstructor
@Tag(name = "Tally", description = "Election Tally Management")
@PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")
public class TallyController {

    private final TallyService tallyService;
    private final ElectionResultMapper electionResultMapper;

    @PostMapping("/{id}/tally")
    @Operation(summary = "Start election tally", description = "Decrypts votes and calculates results. Election must be CLOSED.")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ElectionResultResponse> startTally(
            @PathVariable UUID id,
            Authentication authentication) {
        // In a real scenario, we'd extract user ID from authentication
        // For now using a random UUID or we could parse the token if we had the
        // UserDetails structure
        UUID userId = UUID.randomUUID();

        ElectionResult result = tallyService.performTally(id, userId);
        return ResponseEntity.ok(electionResultMapper.toResponse(result));
    }

    @GetMapping("/{id}/results")
    @Operation(summary = "Get election results", description = "Get results of a tallied election")
    public ResponseEntity<ElectionResultResponse> getResults(@PathVariable UUID id) {
        ElectionResult result = tallyService.getResults(id);
        return ResponseEntity.ok(electionResultMapper.toResponse(result));
    }

    @PostMapping("/{id}/results/publish")
    @Operation(summary = "Publish election results", description = "Make results public")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ElectionResultResponse> publishResults(@PathVariable UUID id) {
        ElectionResult result = tallyService.publishResults(id);
        return ResponseEntity.ok(electionResultMapper.toResponse(result));
    }
}
