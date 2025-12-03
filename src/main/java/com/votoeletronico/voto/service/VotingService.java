package com.votoeletronico.voto.service;

import com.votoeletronico.voto.audit.AuditService;
import com.votoeletronico.voto.domain.audit.AuditEventType;
import com.votoeletronico.voto.domain.election.Candidate;
import com.votoeletronico.voto.domain.election.Election;
import com.votoeletronico.voto.domain.voting.BlindToken;
import com.votoeletronico.voto.domain.voting.EncryptedBallot;
import com.votoeletronico.voto.exception.BusinessException;
import com.votoeletronico.voto.exception.ResourceNotFoundException;
import com.votoeletronico.voto.repository.CandidateRepository;
import com.votoeletronico.voto.repository.ElectionRepository;
import com.votoeletronico.voto.repository.EncryptedBallotRepository;
import com.votoeletronico.voto.service.crypto.CryptoService;
import com.votoeletronico.voto.service.crypto.EncryptedData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for casting and managing encrypted votes
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VotingService {

    private final EncryptedBallotRepository ballotRepository;
    private final ElectionRepository electionRepository;
    private final CandidateRepository candidateRepository;
    private final TokenService tokenService;
    private final CryptoService cryptoService;
    private final AuditService auditService;

    // In production, election keys would be in HSM
    private final Map<UUID, SecretKey> electionEncryptionKeys = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Cast a vote using a blind token
     */
    @Transactional
    public EncryptedBallot castVote(UUID electionId, String tokenValue, UUID candidateId, String ipAddress, String userAgent) {
        log.info("Casting vote for election {} with candidate {}", electionId, candidateId);

        // Validate election exists and is open
        Election election = electionRepository.findById(electionId)
                .orElseThrow(() -> new ResourceNotFoundException("Election", "id", electionId));

        if (!election.isVotingOpen()) {
            throw new BusinessException("Voting is not open for this election. Status: " + election.getStatus());
        }

        // Validate candidate belongs to election
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate", "id", candidateId));

        if (!candidate.getElection().getId().equals(electionId)) {
            throw new BusinessException("Candidate does not belong to this election");
        }

        // Get previous ballot for hash chain
        Optional<EncryptedBallot> previousBallot = ballotRepository.findLastBallot(electionId);
        String prevBallotHash = previousBallot.map(EncryptedBallot::getBallotHash).orElse(null);

        // Encrypt vote
        String voteData = createVoteData(candidateId);
        SecretKey encryptionKey = getOrCreateElectionKey(electionId);
        EncryptedData encrypted = cryptoService.encryptAES(voteData, encryptionKey);

        // Create ballot hash (includes encrypted data + prev hash for chain)
        String ballotContent = encrypted.ciphertext() + encrypted.nonce() + (prevBallotHash != null ? prevBallotHash : "");
        String ballotHash = cryptoService.hashSHA256(ballotContent);

        // Sign ballot for integrity
        String keyId = "election-" + electionId;
        String verificationData = ballotHash + electionId + encrypted.algorithm();
        // In production, use proper signing key from HSM
        String signature = cryptoService.hashSHA256(verificationData); // Simplified for now

        // Create ballot
        EncryptedBallot ballot = EncryptedBallot.builder()
                .election(election)
                .encryptedVote(encrypted.ciphertext())
                .ballotHash(ballotHash)
                .encryptionAlgorithm(encrypted.algorithm())
                .keyId(keyId)
                .nonce(encrypted.nonce())
                .castAt(Instant.now())
                .ipHash(ipAddress != null ? cryptoService.hashSHA256(ipAddress) : null)
                .userAgentHash(userAgent != null ? cryptoService.hashSHA256(userAgent) : null)
                .prevBallotHash(prevBallotHash)
                .verificationSignature(signature)
                .tallied(false)
                .build();

        // Save ballot
        EncryptedBallot saved = ballotRepository.save(ballot);

        // Consume token (marks as used)
        BlindToken consumedToken = tokenService.validateAndConsumeToken(electionId, tokenValue, saved.getId());

        log.info("Vote cast successfully. Ballot ID: {}, Hash: {}", saved.getId(), saved.getBallotHash());

        // Audit log (without linking to voter identity)
        auditService.logEvent(AuditEventType.VOTER_REGISTERED, Map.of(
                "electionId", electionId.toString(),
                "ballotId", saved.getId().toString(),
                "ballotHash", ballotHash,
                "action", "Vote Cast"
        ));

        return saved;
    }

    /**
     * Cast abstention vote
     */
    @Transactional
    public EncryptedBallot castAbstention(UUID electionId, String tokenValue, String justification, String ipAddress, String userAgent) {
        log.info("Casting abstention for election {}", electionId);

        // Validate election exists and is open
        Election election = electionRepository.findById(electionId)
                .orElseThrow(() -> new ResourceNotFoundException("Election", "id", electionId));

        if (!election.isVotingOpen()) {
            throw new BusinessException("Voting is not open for this election");
        }

        if (!election.getAllowAbstention()) {
            throw new BusinessException("Abstention is not allowed for this election");
        }

        if (election.getRequireJustification() && (justification == null || justification.isBlank())) {
            throw new BusinessException("Justification is required for abstention");
        }

        // Get previous ballot for hash chain
        Optional<EncryptedBallot> previousBallot = ballotRepository.findLastBallot(electionId);
        String prevBallotHash = previousBallot.map(EncryptedBallot::getBallotHash).orElse(null);

        // Encrypt abstention
        String voteData = createAbstentionData(justification);
        SecretKey encryptionKey = getOrCreateElectionKey(electionId);
        EncryptedData encrypted = cryptoService.encryptAES(voteData, encryptionKey);

        // Create ballot hash
        String ballotContent = encrypted.ciphertext() + encrypted.nonce() + (prevBallotHash != null ? prevBallotHash : "");
        String ballotHash = cryptoService.hashSHA256(ballotContent);

        // Sign ballot
        String keyId = "election-" + electionId;
        String verificationData = ballotHash + electionId + encrypted.algorithm();
        String signature = cryptoService.hashSHA256(verificationData);

        // Create ballot
        EncryptedBallot ballot = EncryptedBallot.builder()
                .election(election)
                .encryptedVote(encrypted.ciphertext())
                .ballotHash(ballotHash)
                .encryptionAlgorithm(encrypted.algorithm())
                .keyId(keyId)
                .nonce(encrypted.nonce())
                .castAt(Instant.now())
                .ipHash(ipAddress != null ? cryptoService.hashSHA256(ipAddress) : null)
                .userAgentHash(userAgent != null ? cryptoService.hashSHA256(userAgent) : null)
                .prevBallotHash(prevBallotHash)
                .verificationSignature(signature)
                .tallied(false)
                .build();

        // Save ballot
        EncryptedBallot saved = ballotRepository.save(ballot);

        // Consume token
        tokenService.validateAndConsumeToken(electionId, tokenValue, saved.getId());

        log.info("Abstention cast successfully. Ballot ID: {}", saved.getId());

        // Audit log
        auditService.logEvent(AuditEventType.VOTER_REGISTERED, Map.of(
                "electionId", electionId.toString(),
                "ballotId", saved.getId().toString(),
                "ballotHash", ballotHash,
                "action", "Abstention Cast"
        ));

        return saved;
    }

    /**
     * Verify ballot exists (voter receipt verification)
     */
    public boolean verifyBallotReceipt(String ballotHash) {
        return ballotRepository.existsByBallotHash(ballotHash);
    }

    /**
     * Get ballot by hash (for verification)
     */
    public Optional<EncryptedBallot> getBallotByHash(String ballotHash) {
        return ballotRepository.findByBallotHash(ballotHash);
    }

    /**
     * Get voting statistics
     */
    public Map<String, Long> getVotingStatistics(UUID electionId) {
        long totalBallots = ballotRepository.countByElectionId(electionId);
        long talliedBallots = ballotRepository.findByElectionId(electionId).stream()
                .filter(EncryptedBallot::getTallied)
                .count();
        long pendingBallots = totalBallots - talliedBallots;

        return Map.of(
                "totalBallots", totalBallots,
                "talliedBallots", talliedBallots,
                "pendingBallots", pendingBallots
        );
    }

    /**
     * Verify ballot chain integrity
     */
    public boolean verifyBallotChain(UUID electionId) {
        log.info("Verifying ballot chain for election {}", electionId);

        var ballots = ballotRepository.findBallotsForChainVerification(electionId);

        if (ballots.isEmpty()) {
            return true; // Empty chain is valid
        }

        String previousHash = null;
        for (EncryptedBallot ballot : ballots) {
            // First ballot should have null prev hash
            if (previousHash == null && ballot.getPrevBallotHash() != null) {
                log.warn("First ballot has non-null previous hash");
                return false;
            }

            // Subsequent ballots should link to previous
            if (previousHash != null && !previousHash.equals(ballot.getPrevBallotHash())) {
                log.warn("Broken chain at ballot: {}", ballot.getId());
                return false;
            }

            previousHash = ballot.getBallotHash();
        }

        log.info("Ballot chain verified successfully. {} ballots", ballots.size());
        return true;
    }

    /**
     * Create vote data JSON
     */
    private String createVoteData(UUID candidateId) {
        return String.format("""
                {
                    "type": "VOTE",
                    "candidateId": "%s",
                    "timestamp": "%s"
                }
                """, candidateId, Instant.now());
    }

    /**
     * Create abstention data JSON
     */
    private String createAbstentionData(String justification) {
        return String.format("""
                {
                    "type": "ABSTENTION",
                    "justification": "%s",
                    "timestamp": "%s"
                }
                """, justification != null ? justification : "", Instant.now());
    }

    /**
     * Get or create election encryption key
     * In production, use HSM or secure key storage
     */
    private SecretKey getOrCreateElectionKey(UUID electionId) {
        return electionEncryptionKeys.computeIfAbsent(electionId, id -> {
            log.info("Generating new encryption key for election: {}", id);
            return cryptoService.generateAESKey();
        });
    }

    /**
     * Get election encryption key (for decryption during tally)
     * This would be restricted to authorized tally operations only
     */
    public SecretKey getElectionKeyForTally(UUID electionId) {
        SecretKey key = electionEncryptionKeys.get(electionId);
        if (key == null) {
            throw new BusinessException("Encryption key not found for election");
        }
        return key;
    }
}
