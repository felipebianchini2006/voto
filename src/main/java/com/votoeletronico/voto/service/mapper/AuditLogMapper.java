package com.votoeletronico.voto.service.mapper;

import com.votoeletronico.voto.domain.audit.AuditLog;
import com.votoeletronico.voto.dto.response.AuditLogResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AuditLogMapper {

    @Mapping(source = "entryHash", target = "entryHash", qualifiedByName = "bytesToHex")
    @Mapping(source = "prevHash", target = "prevHash", qualifiedByName = "bytesToHex")
    @Mapping(source = "signature", target = "signature", qualifiedByName = "bytesToHex")
    AuditLogResponse toResponse(AuditLog log);

    List<AuditLogResponse> toResponseList(List<AuditLog> logs);

    @Named("bytesToHex")
    default String bytesToHex(byte[] bytes) {
        if (bytes == null)
            return null;
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
