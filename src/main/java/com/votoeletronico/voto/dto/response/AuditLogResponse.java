package com.votoeletronico.voto.dto.response;

import com.votoeletronico.voto.domain.audit.AuditEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {
    private Long id;
    private AuditEventType eventType;
    private String eventData;
    private Instant ts;
    private String entryHash;
    private String prevHash;
    private String signature;
}
