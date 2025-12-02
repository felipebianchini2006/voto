package com.votoeletronico.voto.domain.audit;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

/**
 * Immutable audit log entry with hash chain for integrity
 * Stores all critical events in the system
 */
@Entity
@Table(name = "audit_log", schema = "audit", indexes = {
        @Index(name = "idx_audit_log_ts", columnList = "ts"),
        @Index(name = "idx_audit_log_event_type", columnList = "event_type"),
        @Index(name = "idx_audit_log_entry_hash", columnList = "entry_hash"),
        @Index(name = "idx_audit_log_prev_hash", columnList = "prev_hash")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 100)
    private AuditEventType eventType;

    @NotNull
    @Column(name = "event_data", nullable = false, columnDefinition = "JSONB")
    private String eventData;

    @NotNull
    @Column(name = "entry_hash", nullable = false, length = 64)
    private byte[] entryHash;

    @Column(name = "prev_hash", length = 64)
    private byte[] prevHash;

    @NotNull
    @Column(name = "signature", nullable = false)
    private byte[] signature;

    @NotNull
    @Column(name = "signer_key_id", nullable = false, length = 100)
    private String signerKeyId;

    @NotNull
    @Column(name = "ts", nullable = false)
    @Builder.Default
    private Instant ts = Instant.now();

    /**
     * Check if this entry is linked to a previous entry
     */
    public boolean hasParent() {
        return prevHash != null;
    }

    /**
     * Verify hash chain integrity
     */
    public boolean isChainValid(byte[] expectedPrevHash) {
        if (prevHash == null && expectedPrevHash == null) {
            return true; // First entry
        }
        if (prevHash == null || expectedPrevHash == null) {
            return false;
        }
        return java.util.Arrays.equals(prevHash, expectedPrevHash);
    }
}
