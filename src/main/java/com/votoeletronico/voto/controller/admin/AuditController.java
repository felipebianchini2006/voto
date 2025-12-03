package com.votoeletronico.voto.controller.admin;

import com.votoeletronico.voto.audit.AuditService;
import com.votoeletronico.voto.domain.audit.AuditLog;
import com.votoeletronico.voto.dto.response.AuditLogResponse;
import com.votoeletronico.voto.repository.AuditLogRepository;
import com.votoeletronico.voto.service.mapper.AuditLogMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@Tag(name = "Audit", description = "System Audit Logs")
public class AuditController {

    private final AuditService auditService;
    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;

    @GetMapping("/log")
    @Operation(summary = "Get audit logs", description = "Get paginated audit logs")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogs(Pageable pageable) {
        Page<AuditLog> page = auditLogRepository.findAll(pageable);
        return ResponseEntity.ok(page.map(auditLogMapper::toResponse));
    }

    @GetMapping("/commitment")
    @Operation(summary = "Get current root hash", description = "Get the latest hash of the audit chain")
    public ResponseEntity<Map<String, String>> getCommitment() {
        String rootHash = auditService.getCurrentRootHash();
        return ResponseEntity.ok(Map.of("rootHash", rootHash != null ? rootHash : ""));
    }

    @GetMapping("/verify-chain")
    @Operation(summary = "Verify chain integrity", description = "Verifies the cryptographic integrity of the audit log chain")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")
    public ResponseEntity<Map<String, Object>> verifyChain() {
        boolean valid = auditService.verifyChainIntegrity();
        return ResponseEntity.ok(Map.of(
                "valid", valid,
                "timestamp", java.time.Instant.now()));
    }
}
