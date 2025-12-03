package com.votoeletronico.voto.service.mapper;

import com.votoeletronico.voto.domain.results.CandidateResult;
import com.votoeletronico.voto.domain.results.ElectionResult;
import com.votoeletronico.voto.dto.response.CandidateResultResponse;
import com.votoeletronico.voto.dto.response.ElectionResultResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ElectionResultMapper {

    @Mapping(source = "election.id", target = "electionId")
    @Mapping(source = "election.name", target = "electionName")
    @Mapping(source = "candidateResults", target = "candidates")
    ElectionResultResponse toResponse(ElectionResult result);

    @Mapping(source = "candidate.id", target = "candidateId")
    @Mapping(source = "candidate.name", target = "candidateName")
    @Mapping(source = "candidate.ballotNumber", target = "ballotNumber")
    CandidateResultResponse toCandidateResponse(CandidateResult result);

    List<CandidateResultResponse> toCandidateResponseList(List<CandidateResult> results);
}
