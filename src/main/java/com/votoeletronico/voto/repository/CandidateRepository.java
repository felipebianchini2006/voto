package com.votoeletronico.voto.repository;

import com.votoeletronico.voto.domain.election.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CandidateRepository extends JpaRepository<Candidate, UUID> {

    /**
     * Find all candidates for an election
     */
    List<Candidate> findByElectionIdOrderByBallotNumberAsc(UUID electionId);

    /**
     * Find candidate by election and ballot number
     */
    Optional<Candidate> findByElectionIdAndBallotNumber(UUID electionId, Integer ballotNumber);

    /**
     * Check if ballot number exists in election
     */
    boolean existsByElectionIdAndBallotNumber(UUID electionId, Integer ballotNumber);

    /**
     * Find candidate by election and name
     */
    Optional<Candidate> findByElectionIdAndNameIgnoreCase(UUID electionId, String name);

    /**
     * Count candidates in an election
     */
    long countByElectionId(UUID electionId);

    /**
     * Delete all candidates for an election
     */
    void deleteByElectionId(UUID electionId);

    /**
     * Find next available ballot number for election
     */
    @Query("""
            SELECT COALESCE(MAX(c.ballotNumber), 0) + 1
            FROM Candidate c
            WHERE c.election.id = :electionId
            """)
    Integer findNextBallotNumber(@Param("electionId") UUID electionId);

    /**
     * Search candidates by name
     */
    @Query("""
            SELECT c FROM Candidate c
            WHERE c.election.id = :electionId
            AND LOWER(c.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            ORDER BY c.ballotNumber ASC
            """)
    List<Candidate> searchByName(
            @Param("electionId") UUID electionId,
            @Param("searchTerm") String searchTerm
    );

    // ============================================================================
    // User-related queries (for candidate portal)
    // ============================================================================

    /**
     * Find all candidates created by a specific user
     */
    List<Candidate> findByUserId(UUID userId);

    /**
     * Find candidate by user and election
     */
    Optional<Candidate> findByUserIdAndElectionId(UUID userId, UUID electionId);

    /**
     * Check if user already has a candidate in this election
     */
    boolean existsByUserIdAndElectionId(UUID userId, UUID electionId);

    /**
     * Count active (RUNNING) elections for a user
     * Used to enforce "1 active election per candidate" rule
     */
    @Query("""
            SELECT COUNT(c) FROM Candidate c
            JOIN c.election e
            WHERE c.user.id = :userId
            AND e.status = 'RUNNING'
            """)
    long countActiveElectionsForUser(@Param("userId") UUID userId);
}
