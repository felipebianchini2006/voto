package com.votoeletronico.voto.service.mapper;

import com.votoeletronico.voto.domain.voter.Voter;
import com.votoeletronico.voto.dto.request.RegisterVoterRequest;
import com.votoeletronico.voto.dto.response.VoterResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface VoterMapper {

    /**
     * Convert Voter entity to VoterResponse DTO with masked data
     */
    @Mapping(target = "electionId", source = "election.id")
    @Mapping(target = "externalIdMasked", source = "externalId", qualifiedByName = "maskExternalId")
    @Mapping(target = "emailMasked", source = "email", qualifiedByName = "maskEmail")
    VoterResponse toResponse(Voter voter);

    /**
     * Convert list of Voters to list of VoterResponse DTOs
     */
    List<VoterResponse> toResponseList(List<Voter> voters);

    /**
     * Convert RegisterVoterRequest to Voter entity
     */
    @Mapping(target = "election", ignore = true)
    @Mapping(target = "externalIdHash", ignore = true)
    @Mapping(target = "emailHash", ignore = true)
    @Mapping(target = "registeredAt", ignore = true)
    Voter toEntity(RegisterVoterRequest request);

    /**
     * Mask external ID for privacy (show last 4 digits only)
     */
    @Named("maskExternalId")
    default String maskExternalId(String externalId) {
        if (externalId == null || externalId.length() <= 4) {
            return "****";
        }
        return "****" + externalId.substring(externalId.length() - 4);
    }

    /**
     * Mask email for privacy
     */
    @Named("maskEmail")
    default String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "****";
        }
        String[] parts = email.split("@");
        String localPart = parts[0];
        String domain = parts[1];

        String maskedLocal = localPart.length() > 1
                ? localPart.charAt(0) + "****"
                : "****";

        return maskedLocal + "@" + domain;
    }
}
