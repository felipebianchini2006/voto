package com.votoeletronico.voto.repository;

import com.votoeletronico.voto.domain.audit.AuditLog;
import com.votoeletronico.voto.domain.audit.AuditEventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Find all audit logs ordered by timestamp
     */
    List<AuditLog> findAllByOrderByTsAsc();

    /**
     * Find audit logs by event type
     */
    Page<AuditLog> findByEventType(AuditEventType eventType, Pageable pageable);

    /**
     * Find audit logs in time range
     */
    Page<AuditLog> findByTsBetween(Instant startTs, Instant endTs, Pageable pageable);

    /**
     * Find the last audit log entry (most recent)
     */
    @Query("SELECT a FROM AuditLog a ORDER BY a.id DESC LIMIT 1")
    Optional<AuditLog> findLastEntry();

    /**
     * Count total audit log entries
     */
    long count();

    /**
     * Get audit logs after a specific ID
     */
    List<AuditLog> findByIdGreaterThanOrderByIdAsc(Long id);
}
