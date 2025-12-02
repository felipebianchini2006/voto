package com.votoeletronico.voto.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.votoeletronico.voto.domain.audit.AuditEventType;
import com.votoeletronico.voto.domain.audit.AuditLog;
import com.votoeletronico.voto.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

/**
 * Service for creating and managing audit log entries
 * Implements hash chain for integrity verification
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    private static final String SIGNER_KEY_ID = "system-v1"; // In production, use actual key management

    /**
     * Log an audit event
     * Uses separate transaction to ensure audit logging even if main transaction fails
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logEvent(AuditEventType eventType, Map<String, Object> eventData) {
        try {
            String eventDataJson = objectMapper.writeValueAsString(eventData);

            // Get last entry to chain hashes
            AuditLog lastEntry = auditLogRepository.findLastEntry().orElse(null);
            byte[] prevHash = lastEntry != null ? lastEntry.getEntryHash() : null;

            // Create new entry
            AuditLog entry = AuditLog.builder()
                    .eventType(eventType)
                    .eventData(eventDataJson)
                    .prevHash(prevHash)
                    .signerKeyId(SIGNER_KEY_ID)
                    .ts(Instant.now())
                    .build();

            // Calculate entry hash
            byte[] entryHash = calculateEntryHash(entry);
            entry.setEntryHash(entryHash);

            // Sign the entry (simplified - in production use real digital signatures)
            byte[] signature = signEntry(entry);
            entry.setSignature(signature);

            // Save
            auditLogRepository.save(entry);

            log.debug("Audit event logged: type={}, id={}", eventType, entry.getId());

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize audit event data", e);
            throw new RuntimeException("Failed to log audit event", e);
        }
    }

    /**
     * Convenience method for logging with simple message
     */
    public void logEvent(AuditEventType eventType, String entityType, Object entityId, String action) {
        Map<String, Object> data = Map.of(
                "entityType", entityType,
                "entityId", String.valueOf(entityId),
                "action", action,
                "timestamp", Instant.now().toString()
        );
        logEvent(eventType, data);
    }

    /**
     * Calculate SHA-256 hash of audit entry
     */
    private byte[] calculateEntryHash(AuditLog entry) {
        StringBuilder dataToHash = new StringBuilder();
        dataToHash.append(entry.getEventType().name());
        dataToHash.append("|");
        dataToHash.append(entry.getEventData());
        dataToHash.append("|");
        dataToHash.append(entry.getTs().toString());
        dataToHash.append("|");
        if (entry.getPrevHash() != null) {
            dataToHash.append(bytesToHex(entry.getPrevHash()));
        }

        return DigestUtils.sha256(dataToHash.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Sign audit entry (simplified implementation)
     * In production, use proper digital signatures with private key
     */
    private byte[] signEntry(AuditLog entry) {
        // Simplified signature: HMAC-SHA256 of entry hash
        // In production: use Ed25519 or RSA signatures
        String signatureData = bytesToHex(entry.getEntryHash()) + "|" + entry.getSignerKeyId();
        return DigestUtils.sha256(signatureData.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Verify integrity of audit log chain
     */
    @Transactional(readOnly = true)
    public boolean verifyChainIntegrity() {
        var entries = auditLogRepository.findAllByOrderByTsAsc();

        if (entries.isEmpty()) {
            return true;
        }

        byte[] expectedPrevHash = null;

        for (AuditLog entry : entries) {
            // Verify hash chain
            if (!entry.isChainValid(expectedPrevHash)) {
                log.error("Chain integrity violation at entry ID: {}", entry.getId());
                return false;
            }

            // Verify entry hash
            byte[] calculatedHash = calculateEntryHash(entry);
            if (!java.util.Arrays.equals(calculatedHash, entry.getEntryHash())) {
                log.error("Entry hash mismatch at entry ID: {}", entry.getId());
                return false;
            }

            expectedPrevHash = entry.getEntryHash();
        }

        log.info("Audit log chain integrity verified: {} entries", entries.size());
        return true;
    }

    /**
     * Get current root hash (last entry hash)
     */
    @Transactional(readOnly = true)
    public String getCurrentRootHash() {
        return auditLogRepository.findLastEntry()
                .map(entry -> bytesToHex(entry.getEntryHash()))
                .orElse(null);
    }

    /**
     * Convert bytes to hex string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
