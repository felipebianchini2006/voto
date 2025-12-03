package com.votoeletronico.voto.controller.public_api;

import com.votoeletronico.voto.domain.results.ElectionResult;
import com.votoeletronico.voto.dto.response.ElectionResultResponse;
import com.votoeletronico.voto.service.TallyService;
import com.votoeletronico.voto.service.mapper.ElectionResultMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/public/elections")
@RequiredArgsConstructor
@Tag(name = "Public Results", description = "Public Election Results")
public class PublicResultController {

    private final TallyService tallyService;
    private final ElectionResultMapper electionResultMapper;

    @GetMapping("/{id}/results")
    @Operation(summary = "Get public election results", description = "Get results if they have been published")
    public ResponseEntity<ElectionResultResponse> getPublicResults(@PathVariable UUID id) {
        ElectionResult result = tallyService.getResults(id);

        if (!result.getPublished()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(electionResultMapper.toResponse(result));
    }
}
