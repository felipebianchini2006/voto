package com.votoeletronico.voto.service;

import com.votoeletronico.voto.audit.AuditService;
import com.votoeletronico.voto.domain.audit.AuditEventType;
import com.votoeletronico.voto.domain.election.Election;
import com.votoeletronico.voto.domain.voter.Voter;
import com.votoeletronico.voto.domain.voting.BlindToken;
import com.votoeletronico.voto.domain.voting.TokenStatus;
import com.votoeletronico.voto.exception.BusinessException;
import com.votoeletronico.voto.exception.ResourceNotFoundException;
import com.votoeletronico.voto.repository.BlindTokenRepository;
import com.votoeletronico.voto.repository.ElectionRepository;
import com.votoeletronico.voto.repository.VoterRepository;
import com.votoeletronico.voto.service.crypto.CryptoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Service for managing blind tokens
 * Handles token generation, validation, and lifecycle
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TokenService {

    private final BlindTokenRepository tokenRepository;
    private final ElectionRepository electionRepository;
    private final VoterRepository voterRepository;
    private final CryptoService cryptoService;
    private final AuditService auditService;

    // In a production system, keys would be stored in HSM or secure key management service
    // For now, we'll generate keys per election and store them temporarily
    private final Map<UUID, KeyPair> electionKeys = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Issue a blind token to an eligible voter
     */
    @Transactional
    public BlindToken issueToken(UUID electionId, String voterExternalId) {
        log.info("Issuing token for election {} to voter {}", electionId, voterExternalId);

        // Find election
        Election election = electionRepository.findById(electionId)
                .orElseThrow(() -> new ResourceNotFoundException("Election", "id", electionId));

        // Check election is in RUNNING status
        if (!election.isVotingOpen()) {
            throw new BusinessException("Election is not open for voting. Current status: " + election.getStatus());
        }

        // Find voter
        String voterIdHash = Voter.hashExternalId(voterExternalId);
        Voter voter = voterRepository.findByElectionIdAndExternalIdHash(electionId, voterIdHash)
                .orElseThrow(() -> new BusinessException("Voter not registered for this election"));

        // Check voter eligibility
        if (!voter.isEligible()) {
            throw new BusinessException("Voter is not eligible to vote. Reason: " + voter.getIneligibilityReason());
        }

        // Check if voter already has a token
        if (tokenRepository.existsByElectionIdAndVoterIdHash(electionId, voterIdHash)) {
            throw new BusinessException("Voter already has a token for this election");
        }

        // Generate token
        String tokenValue = cryptoService.generateSecureToken();
        String tokenHash = cryptoService.hashSHA256(tokenValue);

        // Get or create election key pair
        KeyPair keyPair = getOrCreateElectionKeys(electionId);

        // Sign the token
        String signature = cryptoService.signData(tokenHash, keyPair.getPrivate());

        // Generate nonce
        String nonce = cryptoService.generateNonceString();

        // Create token entity
        BlindToken token = BlindToken.builder()
                .election(election)
                .voterIdHash(voterIdHash)
                .tokenHash(tokenHash)
                .signature(signature)
                .status(TokenStatus.ISSUED)
                .issuedAt(Instant.now())
                .expiresAt(election.getEndTs())
                .nonce(nonce)
                .build();

        BlindToken saved = tokenRepository.save(token);
        log.info("Token issued successfully: {}", saved.getId());

        // Audit log
        auditService.logEvent(AuditEventType.VOTER_REGISTERED, Map.of(
                "electionId", electionId.toString(),
                "tokenId", saved.getId().toString(),
                "action", "Token Issued"
        ));

        // In a real system, the tokenValue would be returned to the voter
        // and stored securely by them (not in our database)
        // For now, we'll attach it as a transient field for demonstration
        // Note: In production, use a separate DTO for this
        return saved;
    }

    /**
     * Validate token without consuming it
     */
    public boolean validateToken(String tokenValue) {
        String tokenHash = cryptoService.hashSHA256(tokenValue);
        return tokenRepository.isTokenValid(tokenHash, Instant.now());
    }

    /**
     * Validate and consume token (marks as used)
     */
    @Transactional
    public BlindToken validateAndConsumeToken(UUID electionId, String tokenValue, UUID ballotId) {
        log.info("Validating and consuming token for election {}", electionId);

        String tokenHash = cryptoService.hashSHA256(tokenValue);

        // Find token
        BlindToken token = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BusinessException("Invalid token"));

        // Verify token belongs to election
        if (!token.getElection().getId().equals(electionId)) {
            throw new BusinessException("Token does not belong to this election");
        }

        // Verify signature
        KeyPair keyPair = getOrCreateElectionKeys(electionId);
        boolean signatureValid = cryptoService.verifySignature(
                tokenHash,
                token.getSignature(),
                keyPair.getPublic()
        );

        if (!signatureValid) {
            log.warn("Invalid token signature detected");
            throw new BusinessException("Invalid token signature");
        }

        // Check if token is valid
        if (!token.isValid()) {
            throw new BusinessException("Token is not valid. Status: " + token.getStatus());
        }

        // Consume token
        token.consume(ballotId);
        BlindToken consumed = tokenRepository.save(token);

        log.info("Token consumed successfully: {}", consumed.getId());

        // Audit log
        auditService.logEvent(AuditEventType.VOTER_REGISTERED, Map.of(
                "electionId", electionId.toString(),
                "tokenId", consumed.getId().toString(),
                "ballotId", ballotId.toString(),
                "action", "Token Consumed"
        ));

        return consumed;
    }

    /**
     * Get token statistics for an election
     */
    public Map<String, Long> getTokenStatistics(UUID electionId) {
        long issued = tokenRepository.countByElectionIdAndStatus(electionId, TokenStatus.ISSUED);
        long consumed = tokenRepository.countByElectionIdAndStatus(electionId, TokenStatus.CONSUMED);
        long expired = tokenRepository.countByElectionIdAndStatus(electionId, TokenStatus.EXPIRED);
        long revoked = tokenRepository.countByElectionIdAndStatus(electionId, TokenStatus.REVOKED);
        long total = tokenRepository.countByElectionId(electionId);

        return Map.of(
                "total", total,
                "issued", issued,
                "consumed", consumed,
                "expired", expired,
                "revoked", revoked
        );
    }

    /**
     * Revoke a token (admin action)
     */
    @Transactional
    public void revokeToken(UUID tokenId) {
        log.info("Revoking token: {}", tokenId);

        BlindToken token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new ResourceNotFoundException("Token", "id", tokenId));

        token.revoke();
        tokenRepository.save(token);

        log.info("Token revoked successfully: {}", tokenId);

        // Audit log
        auditService.logEvent(AuditEventType.VOTER_ELIGIBILITY_CHANGED, Map.of(
                "tokenId", tokenId.toString(),
                "action", "Token Revoked"
        ));
    }

    /**
     * Expire old tokens (scheduled task)
     */
    @Transactional
    public int expireOldTokens() {
        log.info("Expiring old tokens");
        var expiredTokens = tokenRepository.findExpiredTokens(Instant.now());
        int count = 0;

        for (BlindToken token : expiredTokens) {
            token.setStatus(TokenStatus.EXPIRED);
            tokenRepository.save(token);
            count++;
        }

        log.info("Expired {} tokens", count);
        return count;
    }

    /**
     * Get or create election key pair
     * In production, this would use HSM or secure key storage
     */
    private KeyPair getOrCreateElectionKeys(UUID electionId) {
        return electionKeys.computeIfAbsent(electionId, id -> {
            log.info("Generating new key pair for election: {}", id);
            return cryptoService.generateRSAKeyPair();
        });
    }

    /**
     * Get election public key (for verification)
     */
    public String getElectionPublicKey(UUID electionId) {
        KeyPair keyPair = getOrCreateElectionKeys(electionId);
        return cryptoService.encodePublicKey(keyPair.getPublic());
    }

    /**
     * Check if voter has token for election
     */
    public boolean hasToken(UUID electionId, String voterExternalId) {
        String voterIdHash = Voter.hashExternalId(voterExternalId);
        return tokenRepository.existsByElectionIdAndVoterIdHash(electionId, voterIdHash);
    }

    /**
     * Get voter's token status
     */
    public TokenStatus getVoterTokenStatus(UUID electionId, String voterExternalId) {
        String voterIdHash = Voter.hashExternalId(voterExternalId);
        return tokenRepository.findByElectionIdAndVoterIdHash(electionId, voterIdHash)
                .map(BlindToken::getStatus)
                .orElse(null);
    }
}
