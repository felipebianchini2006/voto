package com.votoeletronico.voto.repository;

import com.votoeletronico.voto.domain.election.Election;
import com.votoeletronico.voto.domain.election.ElectionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ElectionRepository extends JpaRepository<Election, UUID> {

    /**
     * Find elections by status
     */
    List<Election> findByStatus(ElectionStatus status);

    /**
     * Find elections by status with pagination
     */
    Page<Election> findByStatus(ElectionStatus status, Pageable pageable);

    /**
     * Find all elections ordered by start date
     */
    List<Election> findAllByOrderByStartTsDesc();

    /**
     * Find elections by name containing (case-insensitive)
     */
    Page<Election> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * Find active elections (RUNNING and within time range)
     */
    @Query("""
            SELECT e FROM Election e
            WHERE e.status = 'RUNNING'
            AND :now >= e.startTs
            AND :now < e.endTs
            """)
    List<Election> findActiveElections(@Param("now") Instant now);

    /**
     * Find upcoming elections (DRAFT or RUNNING but not started yet)
     */
    @Query("""
            SELECT e FROM Election e
            WHERE (e.status = 'DRAFT' OR e.status = 'RUNNING')
            AND e.startTs > :now
            ORDER BY e.startTs ASC
            """)
    List<Election> findUpcomingElections(@Param("now") Instant now);

    /**
     * Find past elections (CLOSED or ended)
     */
    @Query("""
            SELECT e FROM Election e
            WHERE e.status = 'CLOSED'
            OR (e.status = 'RUNNING' AND e.endTs < :now)
            ORDER BY e.endTs DESC
            """)
    List<Election> findPastElections(@Param("now") Instant now);

    /**
     * Find election with candidates eagerly loaded
     */
    @Query("""
            SELECT e FROM Election e
            LEFT JOIN FETCH e.candidates
            WHERE e.id = :id
            """)
    Optional<Election> findByIdWithCandidates(@Param("id") UUID id);

    /**
     * Count elections by status
     */
    long countByStatus(ElectionStatus status);

    /**
     * Check if election name exists
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Find elections created by a specific user
     */
    List<Election> findByCreatedBy(UUID userId);

    /**
     * Find elections within a date range
     */
    @Query("""
            SELECT e FROM Election e
            WHERE e.startTs >= :startDate
            AND e.endTs <= :endDate
            ORDER BY e.startTs ASC
            """)
    List<Election> findByDateRange(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );
}
