package com.votoeletronico.voto.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.votoeletronico.voto.domain.election.Candidate;
import com.votoeletronico.voto.domain.election.Election;
import com.votoeletronico.voto.domain.election.ElectionStatus;
import com.votoeletronico.voto.domain.results.CandidateResult;
import com.votoeletronico.voto.domain.results.ElectionResult;
import com.votoeletronico.voto.domain.results.TallyStatus;
import com.votoeletronico.voto.domain.voting.EncryptedBallot;
import com.votoeletronico.voto.exception.BusinessException;
import com.votoeletronico.voto.exception.ResourceNotFoundException;
import com.votoeletronico.voto.repository.CandidateResultRepository;
import com.votoeletronico.voto.repository.ElectionRepository;
import com.votoeletronico.voto.repository.ElectionResultRepository;
import com.votoeletronico.voto.repository.EncryptedBallotRepository;
import com.votoeletronico.voto.service.crypto.CryptoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TallyService {

    private final ElectionRepository electionRepository;
    private final ElectionResultRepository electionResultRepository;
    private final CandidateResultRepository candidateResultRepository;
    private final EncryptedBallotRepository ballotRepository;
    private final VotingService votingService;
    private final CryptoService cryptoService;
    private final ObjectMapper objectMapper;

    /**
     * Perform the tally process for an election
     */
    @Transactional
    public ElectionResult performTally(UUID electionId, UUID userId) {
        log.info("Starting tally for election {}", electionId);

        Election election = electionRepository.findById(electionId)
                .orElseThrow(() -> new ResourceNotFoundException("Election", "id", electionId));

        if (election.getStatus() != ElectionStatus.CLOSED) {
            throw new BusinessException("Election must be CLOSED to perform tally");
        }

        if (electionResultRepository.existsByElectionId(electionId)) {
            throw new BusinessException("Tally already performed for this election");
        }

        // Initialize result
        ElectionResult result = ElectionResult.builder()
                .election(election)
                .status(TallyStatus.IN_PROGRESS)
                .tallyStartedAt(Instant.now())
                .talliedBy(userId)
                .build();

        result = electionResultRepository.save(result);

        try {
            // Get encryption key
            SecretKey key = votingService.getElectionKeyForTally(electionId);

            // Get all ballots
            List<EncryptedBallot> ballots = ballotRepository.findByElectionId(electionId);

            // Initialize counters
            Map<UUID, Long> candidateVotes = new HashMap<>();
            long validVotes = 0;
            long abstentions = 0;
            long invalidVotes = 0;
            long totalBallots = ballots.size();

            // Initialize candidate map for quick lookup
            Map<UUID, Candidate> candidateMap = election.getCandidates().stream()
                    .collect(Collectors.toMap(Candidate::getId, c -> c));

            // Process each ballot
            for (EncryptedBallot ballot : ballots) {
                try {
                    String decryptedJson = cryptoService.decryptAES(
                            ballot.getEncryptedVote(),
                            ballot.getNonce(),
                            key);

                    JsonNode node = objectMapper.readTree(decryptedJson);
                    String type = node.get("type").asText();

                    if ("VOTE".equals(type)) {
                        String candidateIdStr = node.get("candidateId").asText();
                        UUID candidateId = UUID.fromString(candidateIdStr);

                        if (candidateMap.containsKey(candidateId)) {
                            candidateVotes.merge(candidateId, 1L, Long::sum);
                            validVotes++;
                        } else {
                            log.warn("Vote for unknown candidate: {}", candidateId);
                            invalidVotes++;
                        }
                    } else if ("ABSTENTION".equals(type)) {
                        abstentions++;
                    } else {
                        log.warn("Unknown vote type: {}", type);
                        invalidVotes++;
                    }

                    ballot.markAsTallied();
                    ballotRepository.save(ballot);

                } catch (Exception e) {
                    log.error("Failed to process ballot {}", ballot.getId(), e);
                    invalidVotes++;
                }
            }

            // Save candidate results
            List<CandidateResult> candidateResults = new ArrayList<>();
            for (Candidate candidate : election.getCandidates()) {
                long votes = candidateVotes.getOrDefault(candidate.getId(), 0L);

                CandidateResult candidateResult = CandidateResult.builder()
                        .electionResult(result)
                        .candidate(candidate)
                        .voteCount(votes)
                        .build();

                candidateResult.calculatePercentage(validVotes);
                candidateResults.add(candidateResult);
            }

            // Determine winner(s)
            if (!candidateResults.isEmpty()) {
                long maxVotes = candidateResults.stream()
                        .mapToLong(CandidateResult::getVoteCount)
                        .max()
                        .orElse(0);

                candidateResults.stream()
                        .filter(cr -> cr.getVoteCount() == maxVotes && maxVotes > 0)
                        .forEach(CandidateResult::markAsWinner);

                // Sort by votes desc
                candidateResults.sort(Comparator.comparingLong(CandidateResult::getVoteCount).reversed());

                // Assign rank
                int rank = 1;
                for (CandidateResult cr : candidateResults) {
                    cr.setRankPosition(rank++);
                }
            }

            candidateResultRepository.saveAll(candidateResults);

            // Update result totals
            result.setTotalBallots(totalBallots);
            result.setValidVotes(validVotes);
            result.setAbstentions(abstentions);
            result.setInvalidVotes(invalidVotes);
            // Assuming tokens issued logic is handled elsewhere or we can query
            // TokenService
            // For now leaving tokensIssued as 0 or we need to inject TokenRepository

            // Calculate Merkle Root (simplified: hash of all ballot hashes sorted)
            String merkleRoot = calculateMerkleRoot(ballots);

            // Calculate results hash (integrity of the result itself)
            String resultsData = validVotes + ":" + abstentions + ":" + merkleRoot;
            String resultsHash = cryptoService.hashSHA256(resultsData);

            // Sign results (simplified)
            String signature = cryptoService.hashSHA256(resultsHash + electionId);

            result.completeTally(merkleRoot, resultsHash, signature);
            result.setCandidateResults(candidateResults); // for return

            return electionResultRepository.save(result);

        } catch (Exception e) {
            log.error("Tally failed for election {}", electionId, e);
            result.failTally(e.getMessage());
            return electionResultRepository.save(result);
        }
    }

    /**
     * Get results for an election
     */
    public ElectionResult getResults(UUID electionId) {
        return electionResultRepository.findByElectionId(electionId)
                .orElseThrow(() -> new ResourceNotFoundException("ElectionResult", "electionId", electionId));
    }

    /**
     * Publish results
     */
    @Transactional
    public ElectionResult publishResults(UUID electionId) {
        ElectionResult result = getResults(electionId);
        result.publish();
        return electionResultRepository.save(result);
    }

    private String calculateMerkleRoot(List<EncryptedBallot> ballots) {
        if (ballots.isEmpty())
            return null;

        List<String> hashes = ballots.stream()
                .map(EncryptedBallot::getBallotHash)
                .sorted()
                .collect(Collectors.toList());

        StringBuilder combined = new StringBuilder();
        for (String hash : hashes) {
            combined.append(hash);
        }

        return cryptoService.hashSHA256(combined.toString());
    }
}
