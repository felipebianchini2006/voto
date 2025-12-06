package com.votoeletronico.voto.controller.public_api;

import com.votoeletronico.voto.dto.response.ElectionResponse;
import com.votoeletronico.voto.service.ElectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Public API for viewing elections (no authentication required)
 */
@Tag(name = "Public Elections", description = "Public endpoints for viewing elections")
@RestController
@RequestMapping("/api/public/elections")
@RequiredArgsConstructor
public class PublicElectionController {

    private final ElectionService electionService;

    @Operation(summary = "List all elections", description = "Get all elections (public access)")
    @GetMapping
    public ResponseEntity<List<ElectionResponse>> getAllElections() {
        var elections = electionService.getAllElections(PageRequest.of(0, 100));
        return ResponseEntity.ok(elections.getContent());
    }

    @Operation(summary = "Get election by ID", description = "Get election details (public access)")
    @GetMapping("/{id}")
    public ResponseEntity<ElectionResponse> getElectionById(@PathVariable UUID id) {
        var response = electionService.getElectionById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "List active elections", description = "Get all active/running elections")
    @GetMapping("/active")
    public ResponseEntity<List<ElectionResponse>> getActiveElections() {
        var elections = electionService.getActiveElections();
        return ResponseEntity.ok(elections);
    }

    @Operation(summary = "List upcoming elections", description = "Get all upcoming elections")
    @GetMapping("/upcoming")
    public ResponseEntity<List<ElectionResponse>> getUpcomingElections() {
        var elections = electionService.getUpcomingElections();
        return ResponseEntity.ok(elections);
    }
}
