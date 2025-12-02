package com.votoeletronico.voto.service.mapper;

import com.votoeletronico.voto.domain.election.Election;
import com.votoeletronico.voto.dto.request.CreateElectionRequest;
import com.votoeletronico.voto.dto.request.UpdateElectionRequest;
import com.votoeletronico.voto.dto.response.ElectionResponse;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", uses = {CandidateMapper.class})
public interface ElectionMapper {

    /**
     * Convert Election entity to ElectionResponse DTO
     */
    @Mapping(target = "votingOpen", expression = "java(election.isVotingOpen())")
    ElectionResponse toResponse(Election election);

    /**
     * Convert Election entity to ElectionResponse DTO without candidates
     */
    @Mapping(target = "candidates", ignore = true)
    @Mapping(target = "votingOpen", expression = "java(election.isVotingOpen())")
    ElectionResponse toResponseWithoutCandidates(Election election);

    /**
     * Convert list of Elections to list of ElectionResponse DTOs
     */
    @Named("toResponseList")
    default List<ElectionResponse> toResponseList(List<Election> elections) {
        return elections.stream()
                .map(this::toResponseWithoutCandidates)
                .toList();
    }

    /**
     * Convert CreateElectionRequest to Election entity
     */
    Election toEntity(CreateElectionRequest request);

    /**
     * Update Election entity from UpdateElectionRequest
     * Only updates non-null fields
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "candidates", ignore = true)
    void updateEntityFromRequest(UpdateElectionRequest request, @MappingTarget Election election);
}
