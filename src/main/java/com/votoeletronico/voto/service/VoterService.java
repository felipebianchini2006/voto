package com.votoeletronico.voto.service;

import com.votoeletronico.voto.audit.AuditService;
import com.votoeletronico.voto.domain.audit.AuditEventType;
import com.votoeletronico.voto.domain.election.Election;
import com.votoeletronico.voto.domain.voter.Voter;
import com.votoeletronico.voto.dto.request.RegisterVoterRequest;
import com.votoeletronico.voto.dto.response.VoterImportResult;
import com.votoeletronico.voto.dto.response.VoterResponse;
import com.votoeletronico.voto.dto.response.VoterStatsResponse;
import com.votoeletronico.voto.exception.BusinessException;
import com.votoeletronico.voto.exception.ResourceNotFoundException;
import com.votoeletronico.voto.repository.ElectionRepository;
import com.votoeletronico.voto.repository.VoterRepository;
import com.votoeletronico.voto.service.mapper.VoterMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VoterService {

    private final VoterRepository voterRepository;
    private final ElectionRepository electionRepository;
    private final VoterMapper voterMapper;
    private final AuditService auditService;

    /**
     * Register a single voter
     */
    @Transactional
    public VoterResponse registerVoter(UUID electionId, RegisterVoterRequest request) {
        log.info("Registering voter for election {}: {}", electionId, request.externalId());

        Election election = findElectionById(electionId);

        if (!election.canBeModified()) {
            throw new BusinessException("Cannot register voters for non-DRAFT elections. Current status: " + election.getStatus());
        }

        // Check for duplicates by external ID hash
        String externalIdHash = Voter.hashExternalId(request.externalId());
        if (voterRepository.existsByElectionIdAndExternalIdHash(electionId, externalIdHash)) {
            throw new BusinessException("Voter with external ID '" + request.externalId() + "' already registered for this election");
        }

        // Check for duplicates by email hash (if email provided)
        if (request.email() != null) {
            String emailHash = Voter.hashEmail(request.email());
            if (voterRepository.existsByElectionIdAndEmailHash(electionId, emailHash)) {
                throw new BusinessException("Voter with email '" + request.email() + "' already registered for this election");
            }
        }

        Voter voter = voterMapper.toEntity(request);
        voter.setElection(election);

        Voter saved = voterRepository.save(voter);
        log.info("Voter registered successfully with ID: {}", saved.getId());

        // Audit log
        auditService.logEvent(AuditEventType.VOTER_REGISTERED, "Voter", saved.getId(), "Registered");

        return voterMapper.toResponse(saved);
    }

    /**
     * Get voters for an election (paginated)
     */
    public Page<VoterResponse> getVotersByElection(UUID electionId, Pageable pageable) {
        return voterRepository.findByElectionId(electionId, pageable)
                .map(voterMapper::toResponse);
    }

    /**
     * Get voter by ID
     */
    public VoterResponse getVoterById(UUID id) {
        Voter voter = findVoterById(id);
        return voterMapper.toResponse(voter);
    }

    /**
     * Update voter eligibility
     */
    @Transactional
    public VoterResponse updateVoterEligibility(UUID id, boolean eligible, String reason) {
        log.info("Updating voter eligibility: id={}, eligible={}", id, eligible);

        Voter voter = findVoterById(id);

        if (!voter.getElection().canBeModified()) {
            throw new BusinessException("Cannot modify voters for non-DRAFT elections");
        }

        if (eligible) {
            voter.markAsEligible();
        } else {
            voter.markAsIneligible(reason);
        }

        Voter updated = voterRepository.save(voter);
        log.info("Voter eligibility updated: {}", id);

        // Audit log
        auditService.logEvent(AuditEventType.VOTER_ELIGIBILITY_CHANGED, "Voter", id,
                eligible ? "Marked as eligible" : "Marked as ineligible");

        return voterMapper.toResponse(updated);
    }

    /**
     * Delete voter
     */
    @Transactional
    public void deleteVoter(UUID id) {
        log.info("Deleting voter: {}", id);

        Voter voter = findVoterById(id);

        if (!voter.getElection().canBeModified()) {
            throw new BusinessException("Cannot delete voters from non-DRAFT elections");
        }

        voterRepository.delete(voter);
        log.info("Voter deleted successfully: {}", id);
    }

    /**
     * Get voter statistics for an election
     */
    public VoterStatsResponse getVoterStatistics(UUID electionId) {
        Map<String, Long> stats = voterRepository.getVoterStatistics(electionId)
                .orElse(Map.of("total", 0L, "eligible", 0L, "ineligible", 0L));

        Long total = stats.getOrDefault("total", 0L);
        Long eligible = stats.getOrDefault("eligible", 0L);
        Long ineligible = stats.getOrDefault("ineligible", 0L);

        return new VoterStatsResponse(electionId, total, eligible, ineligible);
    }

    /**
     * Import voters from CSV file
     * CSV format: externalId,email,eligible
     */
    @Transactional
    public VoterImportResult importVotersFromCsv(UUID electionId, MultipartFile file) {
        log.info("Importing voters from CSV for election: {}", electionId);

        Election election = findElectionById(electionId);

        if (!election.canBeModified()) {
            throw new BusinessException("Cannot import voters for non-DRAFT elections");
        }

        List<VoterImportResult.ImportError> errors = new ArrayList<>();
        int successCount = 0;
        int lineNumber = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                // Skip header line
                if (firstLine) {
                    firstLine = false;
                    if (line.toLowerCase().contains("externalid") || line.toLowerCase().contains("external_id")) {
                        continue;
                    }
                }

                // Skip empty lines
                if (line.trim().isEmpty()) {
                    continue;
                }

                try {
                    String[] parts = line.split(",");
                    if (parts.length < 1) {
                        errors.add(new VoterImportResult.ImportError(
                                lineNumber, "", "Invalid CSV format: not enough columns"));
                        continue;
                    }

                    String externalId = parts[0].trim();
                    String email = parts.length > 1 ? parts[1].trim() : null;
                    boolean eligible = parts.length > 2 ? Boolean.parseBoolean(parts[2].trim()) : true;

                    if (externalId.isEmpty()) {
                        errors.add(new VoterImportResult.ImportError(
                                lineNumber, "", "External ID cannot be empty"));
                        continue;
                    }

                    // Check if voter already exists
                    String externalIdHash = Voter.hashExternalId(externalId);
                    if (voterRepository.existsByElectionIdAndExternalIdHash(electionId, externalIdHash)) {
                        errors.add(new VoterImportResult.ImportError(
                                lineNumber, externalId, "Voter already registered"));
                        continue;
                    }

                    // Create and save voter
                    Voter voter = new Voter();
                    voter.setElection(election);
                    voter.setExternalId(externalId);
                    voter.setEmail(email != null && !email.isEmpty() ? email : null);
                    voter.setEligible(eligible);

                    voterRepository.save(voter);
                    successCount++;

                } catch (Exception e) {
                    log.warn("Error processing line {}: {}", lineNumber, e.getMessage());
                    errors.add(new VoterImportResult.ImportError(
                            lineNumber, "", "Error: " + e.getMessage()));
                }
            }

        } catch (Exception e) {
            log.error("Error reading CSV file", e);
            throw new BusinessException("Failed to read CSV file: " + e.getMessage());
        }

        int totalProcessed = lineNumber - 1; // Subtract header line
        int failureCount = errors.size();

        log.info("CSV import completed: total={}, success={}, failures={}",
                totalProcessed, successCount, failureCount);

        // Audit log
        auditService.logEvent(AuditEventType.VOTER_REGISTERED, Map.of(
                "electionId", electionId.toString(),
                "action", "CSV Import",
                "totalProcessed", totalProcessed,
                "successCount", successCount,
                "failureCount", failureCount
        ));

        return new VoterImportResult(totalProcessed, successCount, failureCount, errors);
    }

    /**
     * Helper method to find voter by ID
     */
    private Voter findVoterById(UUID id) {
        return voterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Voter", "id", id));
    }

    /**
     * Helper method to find election by ID
     */
    private Election findElectionById(UUID id) {
        return electionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Election", "id", id));
    }
}
