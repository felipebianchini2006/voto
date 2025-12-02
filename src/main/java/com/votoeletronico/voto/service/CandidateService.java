package com.votoeletronico.voto.service;

import com.votoeletronico.voto.domain.election.Candidate;
import com.votoeletronico.voto.domain.election.Election;
import com.votoeletronico.voto.dto.request.CreateCandidateRequest;
import com.votoeletronico.voto.dto.response.CandidateResponse;
import com.votoeletronico.voto.exception.BusinessException;
import com.votoeletronico.voto.exception.ResourceNotFoundException;
import com.votoeletronico.voto.repository.CandidateRepository;
import com.votoeletronico.voto.repository.ElectionRepository;
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
}
