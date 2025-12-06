package com.votoeletronico.voto.service;

import com.votoeletronico.voto.domain.election.Candidate;
import com.votoeletronico.voto.domain.election.Election;
import com.votoeletronico.voto.domain.user.User;
import com.votoeletronico.voto.dto.request.ApplyToElectionRequest;
import com.votoeletronico.voto.dto.request.CreateCandidateRequest;
import com.votoeletronico.voto.dto.request.UpdateCandidateProfileRequest;
import com.votoeletronico.voto.dto.response.CandidateResponse;
import com.votoeletronico.voto.exception.BusinessException;
import com.votoeletronico.voto.exception.ResourceNotFoundException;
import com.votoeletronico.voto.repository.CandidateRepository;
import com.votoeletronico.voto.repository.ElectionRepository;
import com.votoeletronico.voto.repository.UserRepository;
import com.votoeletronico.voto.service.mapper.CandidateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CandidateService {

    private final CandidateRepository candidateRepository;
    private final ElectionRepository electionRepository;
    private final UserRepository userRepository;
    private final CandidateMapper candidateMapper;

    /**
     * Add a candidate to an election
     */
    @Transactional
    public CandidateResponse addCandidate(UUID electionId, CreateCandidateRequest request) {
        log.info("Adding candidate to election {}: {}", electionId, request.name());

        Election election = findElectionById(electionId);

        if (!election.canBeModified()) {
            throw new BusinessException("Cannot add candidates to non-DRAFT elections. Current status: " + election.getStatus());
        }

        // Validate ballot number uniqueness
        if (request.ballotNumber() != null &&
                candidateRepository.existsByElectionIdAndBallotNumber(electionId, request.ballotNumber())) {
            throw new BusinessException("Ballot number " + request.ballotNumber() + " already exists in this election");
        }

        Candidate candidate = candidateMapper.toEntity(request);
        candidate.setElection(election);

        // Auto-assign ballot number if not provided
        if (candidate.getBallotNumber() == null) {
            Integer nextBallotNumber = candidateRepository.findNextBallotNumber(electionId);
            candidate.setBallotNumber(nextBallotNumber);
        }

        Candidate saved = candidateRepository.save(candidate);
        log.info("Candidate added successfully with ID: {}", saved.getId());

        return candidateMapper.toResponse(saved);
    }

    /**
     * Get all candidates for an election
     */
    public List<CandidateResponse> getCandidatesByElection(UUID electionId) {
        List<Candidate> candidates = candidateRepository.findByElectionIdOrderByBallotNumberAsc(electionId);
        return candidateMapper.toResponseList(candidates);
    }

    /**
     * Get candidate by ID
     */
    public CandidateResponse getCandidateById(UUID id) {
        Candidate candidate = findCandidateById(id);
        return candidateMapper.toResponse(candidate);
    }

    /**
     * Update a candidate
     */
    @Transactional
    public CandidateResponse updateCandidate(UUID id, CreateCandidateRequest request) {
        log.info("Updating candidate: {}", id);

        Candidate candidate = findCandidateById(id);

        if (!candidate.canBeModified()) {
            throw new BusinessException("Cannot modify candidates in non-DRAFT elections");
        }

        // Validate ballot number uniqueness if changed
        if (request.ballotNumber() != null &&
                !request.ballotNumber().equals(candidate.getBallotNumber()) &&
                candidateRepository.existsByElectionIdAndBallotNumber(
                        candidate.getElection().getId(), request.ballotNumber())) {
            throw new BusinessException("Ballot number " + request.ballotNumber() + " already exists in this election");
        }

        if (request.name() != null) {
            candidate.setName(request.name());
        }
        if (request.ballotNumber() != null) {
            candidate.setBallotNumber(request.ballotNumber());
        }
        if (request.description() != null) {
            candidate.setDescription(request.description());
        }
        if (request.photoUrl() != null) {
            candidate.setPhotoUrl(request.photoUrl());
        }

        Candidate updated = candidateRepository.save(candidate);
        log.info("Candidate updated successfully: {}", id);

        return candidateMapper.toResponse(updated);
    }

    /**
     * Delete a candidate
     */
    @Transactional
    public void deleteCandidate(UUID id) {
        log.info("Deleting candidate: {}", id);

        Candidate candidate = findCandidateById(id);

        if (!candidate.canBeModified()) {
            throw new BusinessException("Cannot delete candidates from non-DRAFT elections");
        }

        candidateRepository.delete(candidate);
        log.info("Candidate deleted successfully: {}", id);
    }

    /**
     * Delete all candidates for an election
     */
    @Transactional
    public void deleteAllCandidatesForElection(UUID electionId) {
        log.info("Deleting all candidates for election: {}", electionId);

        Election election = findElectionById(electionId);

        if (!election.canBeModified()) {
            throw new BusinessException("Cannot delete candidates from non-DRAFT elections");
        }

        candidateRepository.deleteByElectionId(electionId);
        log.info("All candidates deleted for election: {}", electionId);
    }

    /**
     * Helper method to find candidate by ID
     */
    private Candidate findCandidateById(UUID id) {
        return candidateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate", "id", id));
    }

    /**
     * Helper method to find election by ID
     */
    private Election findElectionById(UUID id) {
        return electionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Election", "id", id));
    }

    // ============================================================================
    // Candidate Portal Methods
    // ============================================================================

    /**
     * Candidate applies to an election
     */
    @Transactional
    public CandidateResponse applyToElection(UUID userId, UUID electionId, ApplyToElectionRequest request) {
        log.info("User {} applying to election {}", userId, electionId);

        Election election = findElectionById(electionId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Validate user is a candidate
        if (!user.isCandidate()) {
            throw new BusinessException("Only users with CANDIDATE role can apply to elections");
        }

        // Check if already applied
        if (candidateRepository.existsByUserIdAndElectionId(userId, electionId)) {
            throw new BusinessException("You have already applied to this election");
        }

        // Check if in active election (database trigger will also enforce this)
        long activeCount = candidateRepository.countActiveElectionsForUser(userId);
        if (activeCount > 0 && election.isRunning()) {
            throw new BusinessException("You can only participate in one active election at a time");
        }

        // Check 10 candidate limit (database trigger will also enforce this)
        long candidateCount = candidateRepository.countByElectionId(electionId);
        if (candidateCount >= 10) {
            throw new BusinessException("This election has reached the maximum of 10 candidates");
        }

        // Get next ballot number
        Integer ballotNumber = candidateRepository.findNextBallotNumber(electionId);

        // Create candidate
        Candidate candidate = Candidate.builder()
                .election(election)
                .user(user)
                .name(request.fullName())
                .ballotNumber(ballotNumber)
                .description(request.description())
                .party(request.party())
                .photoUrl(request.photoUrl())
                .build();

        Candidate saved = candidateRepository.save(candidate);
        log.info("Candidate created for user {}: {}", userId, saved.getId());

        return candidateMapper.toResponse(saved);
    }

    /**
     * Withdraw from election
     */
    @Transactional
    public void withdrawFromElection(UUID userId, UUID electionId) {
        log.info("User {} withdrawing from election {}", userId, electionId);

        Candidate candidate = candidateRepository.findByUserIdAndElectionId(userId, electionId)
                .orElseThrow(() -> new BusinessException("You are not registered for this election"));

        Election election = candidate.getElection();
        if (!election.isDraft()) {
            throw new BusinessException("Can only withdraw from DRAFT elections");
        }

        candidateRepository.delete(candidate);
        log.info("Candidate withdrawn: {}", candidate.getId());
    }

    /**
     * Update candidate profile
     */
    @Transactional
    public CandidateResponse updateCandidateProfile(UUID userId, UUID candidateId,
                                                      UpdateCandidateProfileRequest request) {
        log.info("User {} updating candidate profile {}", userId, candidateId);

        Candidate candidate = findCandidateById(candidateId);

        // Verify ownership
        if (!candidate.belongsToUser(userId)) {
            throw new BusinessException("You can only update your own profile");
        }

        // Can only update if election is DRAFT or RUNNING (profile updates allowed during campaign)
        if (candidate.getElection().isClosed() || candidate.getElection().isCancelled()) {
            throw new BusinessException("Cannot update profile for closed or cancelled elections");
        }

        // Update allowed fields
        if (request.fullName() != null) {
            candidate.setName(request.fullName());
        }
        if (request.photoUrl() != null) {
            candidate.setPhotoUrl(request.photoUrl());
        }
        if (request.description() != null) {
            candidate.setDescription(request.description());
        }
        if (request.party() != null) {
            candidate.setParty(request.party());
        }

        Candidate updated = candidateRepository.save(candidate);
        log.info("Candidate profile updated: {}", candidateId);

        return candidateMapper.toResponse(updated);
    }

    /**
     * Get all candidates for a user
     */
    public List<CandidateResponse> getCandidatesForUser(UUID userId) {
        List<Candidate> candidates = candidateRepository.findByUserId(userId);
        return candidates.stream()
                .map(candidateMapper::toResponse)
                .toList();
    }

    /**
     * Find candidate by user ID and election ID
     */
    public Candidate findCandidateByUserAndElection(UUID userId, UUID electionId) {
        return candidateRepository.findByUserIdAndElectionId(userId, electionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Candidate not found for user " + userId + " in election " + electionId));
    }

    /**
     * Get candidate by user ID and election ID (Response DTO)
     */
    public CandidateResponse getCandidateByUserAndElection(UUID userId, UUID electionId) {
        Candidate candidate = findCandidateByUserAndElection(userId, electionId);
        return candidateMapper.toResponse(candidate);
    }
}
