package com.votoeletronico.voto.repository;

import com.votoeletronico.voto.domain.voter.Voter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VoterRepository extends JpaRepository<Voter, UUID> {

    /**
     * Find all voters for an election
     */
    Page<Voter> findByElectionId(UUID electionId, Pageable pageable);

    /**
     * Find voter by election and external ID hash
     */
    Optional<Voter> findByElectionIdAndExternalIdHash(UUID electionId, String externalIdHash);

    /**
     * Find voter by election and email hash
     */
    Optional<Voter> findByElectionIdAndEmailHash(UUID electionId, String emailHash);

    /**
     * Check if voter exists in election by external ID hash
     */
    boolean existsByElectionIdAndExternalIdHash(UUID electionId, String externalIdHash);

    /**
     * Check if voter exists in election by email hash
     */
    boolean existsByElectionIdAndEmailHash(UUID electionId, String emailHash);

    /**
     * Find eligible voters for an election
     */
    List<Voter> findByElectionIdAndEligibleTrue(UUID electionId);

    /**
     * Find ineligible voters for an election
     */
    List<Voter> findByElectionIdAndEligibleFalse(UUID electionId);

    /**
     * Count voters in an election
     */
    long countByElectionId(UUID electionId);

    /**
     * Count eligible voters in an election
     */
    long countByElectionIdAndEligibleTrue(UUID electionId);

    /**
     * Count ineligible voters in an election
     */
    long countByElectionIdAndEligibleFalse(UUID electionId);

    /**
     * Delete all voters for an election
     */
    void deleteByElectionId(UUID electionId);

    /**
     * Get voter statistics for an election
     */
    @Query("""
            SELECT new map(
                COUNT(v) as total,
                SUM(CASE WHEN v.eligible = true THEN 1 ELSE 0 END) as eligible,
                SUM(CASE WHEN v.eligible = false THEN 1 ELSE 0 END) as ineligible
            )
            FROM Voter v
            WHERE v.election.id = :electionId
            """)
    Optional<java.util.Map<String, Long>> getVoterStatistics(@Param("electionId") UUID electionId);
}
