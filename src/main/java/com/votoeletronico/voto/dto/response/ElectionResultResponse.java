package com.votoeletronico.voto.dto.response;

import com.votoeletronico.voto.domain.results.TallyStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ElectionResultResponse {
    private UUID id;
    private UUID electionId;
    private String electionName;
    private TallyStatus status;
    private Instant tallyStartedAt;
    private Instant tallyCompletedAt;
    private Long totalBallots;
    private Long validVotes;
    private Long abstentions;
    private Long invalidVotes;
    private Double turnoutPercentage;
    private String merkleRoot;
    private String resultsHash;
    private Boolean published;
    private Instant publishedAt;
    private List<CandidateResultResponse> candidates;
}
