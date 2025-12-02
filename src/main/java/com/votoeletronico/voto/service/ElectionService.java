package com.votoeletronico.voto.service;

import com.votoeletronico.voto.audit.AuditService;
import com.votoeletronico.voto.domain.audit.AuditEventType;
import com.votoeletronico.voto.domain.election.Election;
import com.votoeletronico.voto.domain.election.ElectionStatus;
import com.votoeletronico.voto.dto.request.CreateElectionRequest;
import com.votoeletronico.voto.dto.request.UpdateElectionRequest;
import com.votoeletronico.voto.dto.response.ElectionResponse;
import com.votoeletronico.voto.exception.BusinessException;
import com.votoeletronico.voto.exception.ResourceNotFoundException;
import com.votoeletronico.voto.repository.ElectionRepository;
import com.votoeletronico.voto.service.mapper.ElectionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ElectionService {

    private final ElectionRepository electionRepository;
    private final ElectionMapper electionMapper;
    private final AuditService auditService;

    /**
     * Create a new election
     */
    @Transactional
    public ElectionResponse createElection(CreateElectionRequest request, UUID createdBy) {
        log.info("Creating new election: {}", request.name());

        // Validate unique name
        if (electionRepository.existsByNameIgnoreCase(request.name())) {
            throw new BusinessException("Election with name '" + request.name() + "' already exists");
        }

        Election election = electionMapper.toEntity(request);
        election.setCreatedBy(createdBy);

        Election saved = electionRepository.save(election);
        log.info("Election created successfully with ID: {}", saved.getId());

        // Audit log
        auditService.logEvent(AuditEventType.ELECTION_CREATED, "Election", saved.getId(), "Created");

        return electionMapper.toResponse(saved);
    }

    /**
     * Get election by ID
     */
    public ElectionResponse getElectionById(UUID id) {
        Election election = findElectionById(id);
        return electionMapper.toResponse(election);
    }

    /**
     * Get election with candidates
     */
    public ElectionResponse getElectionWithCandidates(UUID id) {
        Election election = electionRepository.findByIdWithCandidates(id)
                .orElseThrow(() -> new ResourceNotFoundException("Election", "id", id));
        return electionMapper.toResponse(election);
    }

    /**
     * Get all elections
     */
    public Page<ElectionResponse> getAllElections(Pageable pageable) {
        return electionRepository.findAll(pageable)
                .map(electionMapper::toResponseWithoutCandidates);
    }

    /**
     * Get elections by status
     */
    public Page<ElectionResponse> getElectionsByStatus(ElectionStatus status, Pageable pageable) {
        return electionRepository.findByStatus(status, pageable)
                .map(electionMapper::toResponseWithoutCandidates);
    }

    /**
     * Search elections by name
     */
    public Page<ElectionResponse> searchElections(String name, Pageable pageable) {
        return electionRepository.findByNameContainingIgnoreCase(name, pageable)
                .map(electionMapper::toResponseWithoutCandidates);
    }

    /**
     * Get active elections (currently running)
     */
    public List<ElectionResponse> getActiveElections() {
        List<Election> elections = electionRepository.findActiveElections(Instant.now());
        return electionMapper.toResponseList(elections);
    }

    /**
     * Get upcoming elections
     */
    public List<ElectionResponse> getUpcomingElections() {
        List<Election> elections = electionRepository.findUpcomingElections(Instant.now());
        return electionMapper.toResponseList(elections);
    }

    /**
     * Get past elections
     */
    public List<ElectionResponse> getPastElections() {
        List<Election> elections = electionRepository.findPastElections(Instant.now());
        return electionMapper.toResponseList(elections);
    }

    /**
     * Update election (only DRAFT elections can be updated)
     */
    @Transactional
    public ElectionResponse updateElection(UUID id, UpdateElectionRequest request) {
        log.info("Updating election: {}", id);

        Election election = findElectionById(id);

        if (!election.canBeModified()) {
            throw new BusinessException("Only DRAFT elections can be modified. Current status: " + election.getStatus());
        }

        // Validate name uniqueness if changed
        if (request.name() != null && !request.name().equalsIgnoreCase(election.getName())) {
            if (electionRepository.existsByNameIgnoreCase(request.name())) {
                throw new BusinessException("Election with name '" + request.name() + "' already exists");
            }
        }

        electionMapper.updateEntityFromRequest(request, election);

        Election updated = electionRepository.save(election);
        log.info("Election updated successfully: {}", id);

        return electionMapper.toResponse(updated);
    }

    /**
     * Start an election (change status from DRAFT to RUNNING)
     */
    @Transactional
    public ElectionResponse startElection(UUID id) {
        log.info("Starting election: {}", id);

        Election election = findElectionById(id);

        if (!election.canBeStarted()) {
            throw new BusinessException("Election cannot be started. " +
                    "Requirements: must be DRAFT, have candidates, and valid dates. " +
                    "Current status: " + election.getStatus());
        }

        election.setStatus(ElectionStatus.RUNNING);
        Election updated = electionRepository.save(election);

        log.info("Election started successfully: {}", id);
        return electionMapper.toResponse(updated);
    }

    /**
     * Close an election (change status from RUNNING to CLOSED)
     */
    @Transactional
    public ElectionResponse closeElection(UUID id) {
        log.info("Closing election: {}", id);

        Election election = findElectionById(id);

        if (!election.canBeClosed()) {
            throw new BusinessException("Only RUNNING elections can be closed. Current status: " + election.getStatus());
        }

        election.setStatus(ElectionStatus.CLOSED);
        Election updated = electionRepository.save(election);

        log.info("Election closed successfully: {}", id);
        return electionMapper.toResponse(updated);
    }

    /**
     * Cancel an election
     */
    @Transactional
    public ElectionResponse cancelElection(UUID id) {
        log.info("Cancelling election: {}", id);

        Election election = findElectionById(id);

        if (election.isClosed()) {
            throw new BusinessException("Cannot cancel a CLOSED election");
        }

        election.setStatus(ElectionStatus.CANCELLED);
        Election updated = electionRepository.save(election);

        log.info("Election cancelled successfully: {}", id);
        return electionMapper.toResponse(updated);
    }

    /**
     * Delete an election (only DRAFT elections can be deleted)
     */
    @Transactional
    public void deleteElection(UUID id) {
        log.info("Deleting election: {}", id);

        Election election = findElectionById(id);

        if (!election.isDraft()) {
            throw new BusinessException("Only DRAFT elections can be deleted. Current status: " + election.getStatus());
        }

        electionRepository.delete(election);
        log.info("Election deleted successfully: {}", id);
    }

    /**
     * Helper method to find election by ID or throw exception
     */
    private Election findElectionById(UUID id) {
        return electionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Election", "id", id));
    }
}
