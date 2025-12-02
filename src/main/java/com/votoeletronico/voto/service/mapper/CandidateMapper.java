package com.votoeletronico.voto.service.mapper;

import com.votoeletronico.voto.domain.election.Candidate;
import com.votoeletronico.voto.dto.request.CreateCandidateRequest;
import com.votoeletronico.voto.dto.response.CandidateResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CandidateMapper {

    /**
     * Convert Candidate entity to CandidateResponse DTO
     */
    @Mapping(target = "electionId", source = "election.id")
    CandidateResponse toResponse(Candidate candidate);

    /**
     * Convert list of Candidates to list of CandidateResponse DTOs
     */
    List<CandidateResponse> toResponseList(List<Candidate> candidates);

    /**
     * Convert CreateCandidateRequest to Candidate entity
     */
    @Mapping(target = "election", ignore = true)
    Candidate toEntity(CreateCandidateRequest request);
}
