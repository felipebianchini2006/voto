package com.votoeletronico.voto.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateResultResponse {
    private UUID candidateId;
    private String candidateName;
    private Integer ballotNumber;
    private Long voteCount;
    private Double percentage;
    private Integer rankPosition;
    private Boolean isWinner;
}
