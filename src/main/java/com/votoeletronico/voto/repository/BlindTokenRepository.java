package com.votoeletronico.voto.repository;

import com.votoeletronico.voto.domain.voting.BlindToken;
import com.votoeletronico.voto.domain.voting.TokenStatus;
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
public interface BlindTokenRepository extends JpaRepository<BlindToken, UUID> {

    /**
     * Find token by hash
     */
    Optional<BlindToken> findByTokenHash(String tokenHash);

    /**
     * Check if voter already has a token for this election
     */
    boolean existsByElectionIdAndVoterIdHash(UUID electionId, String voterIdHash);

    /**
     * Find token by voter hash and election
     */
    Optional<BlindToken> findByElectionIdAndVoterIdHash(UUID electionId, String voterIdHash);

    /**
     * Find all tokens for an election
     */
    Page<BlindToken> findByElectionId(UUID electionId, Pageable pageable);

    /**
     * Find tokens by status
     */
    List<BlindToken> findByStatus(TokenStatus status);

    /**
     * Find tokens by election and status
     */
    List<BlindToken> findByElectionIdAndStatus(UUID electionId, TokenStatus status);

    /**
     * Count tokens by election and status
     */
    long countByElectionIdAndStatus(UUID electionId, TokenStatus status);

    /**
     * Find expired tokens that need status update
     */
    @Query("SELECT t FROM BlindToken t WHERE t.status = 'ISSUED' AND t.expiresAt < :now")
    List<BlindToken> findExpiredTokens(@Param("now") Instant now);

    /**
     * Get token statistics for an election
     */
    @Query("""
            SELECT t.status as status, COUNT(t) as count
            FROM BlindToken t
            WHERE t.election.id = :electionId
            GROUP BY t.status
            """)
    List<Object[]> getTokenStatistics(@Param("electionId") UUID electionId);

    /**
     * Check if token exists and is valid
     */
    @Query("""
            SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END
            FROM BlindToken t
            WHERE t.tokenHash = :tokenHash
            AND t.status = 'ISSUED'
            AND t.expiresAt > :now
            """)
    boolean isTokenValid(@Param("tokenHash") String tokenHash, @Param("now") Instant now);

    /**
     * Find tokens that were consumed for a specific ballot
     */
    Optional<BlindToken> findByBallotId(UUID ballotId);

    /**
     * Count total tokens issued for election
     */
    long countByElectionId(UUID electionId);

    /**
     * Find tokens by nonce (for replay prevention)
     */
    boolean existsByNonce(String nonce);
}
