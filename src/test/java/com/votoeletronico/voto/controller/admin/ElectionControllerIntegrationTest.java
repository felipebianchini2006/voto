package com.votoeletronico.voto.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.votoeletronico.voto.BaseIntegrationTest;
import com.votoeletronico.voto.domain.election.Election;
import com.votoeletronico.voto.domain.election.ElectionStatus;
import com.votoeletronico.voto.dto.request.CreateElectionRequest;
import com.votoeletronico.voto.repository.ElectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("ElectionController Integration Tests")
class ElectionControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ElectionRepository electionRepository;

    @BeforeEach
    void cleanUp() {
        electionRepository.deleteAll();
    }

    @Test
    @DisplayName("Should create election via API")
    void shouldCreateElection() throws Exception {
        // Given
        Instant now = Instant.now();
        CreateElectionRequest request = new CreateElectionRequest(
                "Integration Test Election",
                "Test Description",
                now.plus(1, ChronoUnit.DAYS),
                now.plus(2, ChronoUnit.DAYS),
                1,
                true,
                false
        );

        // When/Then
        mockMvc.perform(post("/api/admin/elections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(request.name()))
                .andExpect(jsonPath("$.status").value("DRAFT"));

        // Verify database
        assertThat(electionRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should get election by ID")
    void shouldGetElectionById() throws Exception {
        // Given
        Instant now = Instant.now();
        Election election = Election.builder()
                .name("Test Election")
                .description("Test")
                .startTs(now.plus(1, ChronoUnit.DAYS))
                .endTs(now.plus(2, ChronoUnit.DAYS))
                .status(ElectionStatus.DRAFT)
                .maxVotesPerVoter(1)
                .allowAbstention(true)
                .requireJustification(false)
                .build();

        Election saved = electionRepository.save(election);

        // When/Then
        mockMvc.perform(get("/api/admin/elections/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId().toString()))
                .andExpect(jsonPath("$.name").value("Test Election"));
    }

    @Test
    @DisplayName("Should return 404 when election not found")
    void shouldReturn404WhenNotFound() throws Exception {
        // Given
        String nonExistentId = "00000000-0000-0000-0000-000000000000";

        // When/Then
        mockMvc.perform(get("/api/admin/elections/{id}", nonExistentId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should delete DRAFT election")
    void shouldDeleteDraftElection() throws Exception {
        // Given
        Instant now = Instant.now();
        Election election = Election.builder()
                .name("To Delete")
                .startTs(now.plus(1, ChronoUnit.DAYS))
                .endTs(now.plus(2, ChronoUnit.DAYS))
                .status(ElectionStatus.DRAFT)
                .maxVotesPerVoter(1)
                .allowAbstention(true)
                .requireJustification(false)
                .build();

        Election saved = electionRepository.save(election);

        // When/Then
        mockMvc.perform(delete("/api/admin/elections/{id}", saved.getId()))
                .andExpect(status().isNoContent());

        assertThat(electionRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    @DisplayName("Should validate required fields")
    void shouldValidateRequiredFields() throws Exception {
        // Given
        CreateElectionRequest invalidRequest = new CreateElectionRequest(
                "", // Empty name
                "Test",
                null, // Null start
                null, // Null end
                1,
                true,
                false
        );

        // When/Then
        mockMvc.perform(post("/api/admin/elections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}
