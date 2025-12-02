package com.votoeletronico.voto.service;

import com.votoeletronico.voto.audit.AuditService;
import com.votoeletronico.voto.domain.election.Election;
import com.votoeletronico.voto.domain.election.ElectionStatus;
import com.votoeletronico.voto.dto.request.CreateElectionRequest;
import com.votoeletronico.voto.dto.response.ElectionResponse;
import com.votoeletronico.voto.exception.BusinessException;
import com.votoeletronico.voto.exception.ResourceNotFoundException;
import com.votoeletronico.voto.repository.ElectionRepository;
import com.votoeletronico.voto.service.mapper.ElectionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ElectionService Tests")
class ElectionServiceTest {

    @Mock
    private ElectionRepository electionRepository;

    @Mock
    private ElectionMapper electionMapper;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private ElectionService electionService;

    private CreateElectionRequest validRequest;
    private Election validElection;
    private ElectionResponse validResponse;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        Instant now = Instant.now();
        Instant startTs = now.plus(1, ChronoUnit.DAYS);
        Instant endTs = now.plus(2, ChronoUnit.DAYS);

        validRequest = new CreateElectionRequest(
                "Test Election",
                "Test Description",
                startTs,
                endTs,
                1,
                true,
                false
        );

        validElection = Election.builder()
                .name("Test Election")
                .description("Test Description")
                .startTs(startTs)
                .endTs(endTs)
                .status(ElectionStatus.DRAFT)
                .maxVotesPerVoter(1)
                .allowAbstention(true)
                .requireJustification(false)
                .createdBy(userId)
                .build();
        validElection.setId(UUID.randomUUID());

        validResponse = new ElectionResponse(
                validElection.getId(),
                validElection.getName(),
                validElection.getDescription(),
                validElection.getStartTs(),
                validElection.getEndTs(),
                validElection.getStatus(),
                validElection.getMaxVotesPerVoter(),
                validElection.getAllowAbstention(),
                validElection.getRequireJustification(),
                validElection.getCreatedBy(),
                Instant.now(),
                Instant.now(),
                null,
                false
        );
    }

    @Test
    @DisplayName("Should create election successfully")
    void shouldCreateElectionSuccessfully() {
        // Given
        when(electionRepository.existsByNameIgnoreCase(validRequest.name())).thenReturn(false);
        when(electionMapper.toEntity(validRequest)).thenReturn(validElection);
        when(electionRepository.save(any(Election.class))).thenReturn(validElection);
        when(electionMapper.toResponse(validElection)).thenReturn(validResponse);

        // When
        ElectionResponse response = electionService.createElection(validRequest, userId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo(validRequest.name());
        assertThat(response.status()).isEqualTo(ElectionStatus.DRAFT);

        verify(electionRepository).existsByNameIgnoreCase(validRequest.name());
        verify(electionRepository).save(any(Election.class));
        verify(auditService).logEvent(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should throw exception when election name already exists")
    void shouldThrowExceptionWhenNameExists() {
        // Given
        when(electionRepository.existsByNameIgnoreCase(validRequest.name())).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> electionService.createElection(validRequest, userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already exists");

        verify(electionRepository).existsByNameIgnoreCase(validRequest.name());
        verify(electionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should find election by ID")
    void shouldFindElectionById() {
        // Given
        UUID electionId = validElection.getId();
        when(electionRepository.findById(electionId)).thenReturn(Optional.of(validElection));
        when(electionMapper.toResponse(validElection)).thenReturn(validResponse);

        // When
        ElectionResponse response = electionService.getElectionById(electionId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(electionId);

        verify(electionRepository).findById(electionId);
    }

    @Test
    @DisplayName("Should throw exception when election not found")
    void shouldThrowExceptionWhenElectionNotFound() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(electionRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> electionService.getElectionById(nonExistentId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Election");

        verify(electionRepository).findById(nonExistentId);
    }

    @Test
    @DisplayName("Should delete DRAFT election")
    void shouldDeleteDraftElection() {
        // Given
        UUID electionId = validElection.getId();
        when(electionRepository.findById(electionId)).thenReturn(Optional.of(validElection));

        // When
        electionService.deleteElection(electionId);

        // Then
        verify(electionRepository).findById(electionId);
        verify(electionRepository).delete(validElection);
    }

    @Test
    @DisplayName("Should not delete non-DRAFT election")
    void shouldNotDeleteNonDraftElection() {
        // Given
        validElection.setStatus(ElectionStatus.RUNNING);
        UUID electionId = validElection.getId();
        when(electionRepository.findById(electionId)).thenReturn(Optional.of(validElection));

        // When/Then
        assertThatThrownBy(() -> electionService.deleteElection(electionId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("DRAFT");

        verify(electionRepository).findById(electionId);
        verify(electionRepository, never()).delete(any());
    }
}
