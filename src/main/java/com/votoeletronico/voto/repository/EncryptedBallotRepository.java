package com.votoeletronico.voto.repository;

import com.votoeletronico.voto.domain.voting.EncryptedBallot;
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
public interface EncryptedBallotRepository extends JpaRepository<EncryptedBallot, UUID> {

    /**
     * Find ballot by hash (voter receipt verification)
     */
    Optional<EncryptedBallot> findByBallotHash(String ballotHash);

    /**
     * Check if ballot hash exists
     */
    boolean existsByBallotHash(String ballotHash);

    /**
     * Find all ballots for an election
     */
    Page<EncryptedBallot> findByElectionId(UUID electionId, Pageable pageable);

    /**
     * Find ballots by election (list)
     */
    List<EncryptedBallot> findByElectionId(UUID electionId);

    /**
     * Count total ballots for an election
     */
    long countByElectionId(UUID electionId);

    /**
     * Count ballots cast in time range
     */
    @Query("""
            SELECT COUNT(b)
            FROM EncryptedBallot b
            WHERE b.election.id = :electionId
            AND b.castAt BETWEEN :start AND :end
            """)
    long countBallotsInTimeRange(
            @Param("electionId") UUID electionId,
            @Param("start") Instant start,
            @Param("end") Instant end
    );

    /**
     * Find untallied ballots for an election
     */
    @Query("""
            SELECT b FROM EncryptedBallot b
            WHERE b.election.id = :electionId
            AND b.tallied = false
            ORDER BY b.castAt ASC
            """)
    List<EncryptedBallot> findUntalliedBallots(@Param("electionId") UUID electionId);

    /**
     * Find last ballot (for hash chain)
     */
    @Query("""
            SELECT b FROM EncryptedBallot b
            WHERE b.election.id = :electionId
            ORDER BY b.castAt DESC
            LIMIT 1
            """)
    Optional<EncryptedBallot> findLastBallot(@Param("electionId") UUID electionId);

    /**
     * Get ballot statistics for election
     */
    @Query("""
            SELECT
                COUNT(b) as total,
                COUNT(CASE WHEN b.tallied = true THEN 1 END) as tallied,
                COUNT(CASE WHEN b.tallied = false THEN 1 END) as pending
            FROM EncryptedBallot b
            WHERE b.election.id = :electionId
            """)
    Object[] getBallotStatistics(@Param("electionId") UUID electionId);

    /**
     * Find ballots by key ID (for key rotation scenarios)
     */
    List<EncryptedBallot> findByElectionIdAndKeyId(UUID electionId, String keyId);

    /**
     * Verify ballot chain integrity
     */
    @Query("""
            SELECT b FROM EncryptedBallot b
            WHERE b.election.id = :electionId
            ORDER BY b.castAt ASC
            """)
    List<EncryptedBallot> findBallotsForChainVerification(@Param("electionId") UUID electionId);

    /**
     * Get ballots cast per hour (for monitoring)
     */
    @Query(value = """
            SELECT
                date_trunc('hour', cast_at) as hour,
                COUNT(*) as count
            FROM encrypted_ballots
            WHERE election_id = :electionId
            GROUP BY date_trunc('hour', cast_at)
            ORDER BY hour
            """, nativeQuery = true)
    List<Object[]> getBallotsPerHour(@Param("electionId") UUID electionId);
}
