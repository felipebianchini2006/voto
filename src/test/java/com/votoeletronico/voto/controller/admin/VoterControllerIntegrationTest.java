package com.votoeletronico.voto.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.votoeletronico.voto.BaseIntegrationTest;
import com.votoeletronico.voto.domain.election.Election;
import com.votoeletronico.voto.domain.election.ElectionStatus;
import com.votoeletronico.voto.dto.request.RegisterVoterRequest;
import com.votoeletronico.voto.repository.ElectionRepository;
import com.votoeletronico.voto.repository.VoterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("VoterController Integration Tests")
class VoterControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ElectionRepository electionRepository;

    @Autowired
    private VoterRepository voterRepository;

    private Election testElection;

    @BeforeEach
    void setUp() {
        voterRepository.deleteAll();
        electionRepository.deleteAll();

        // Create test election
        Instant now = Instant.now();
        testElection = Election.builder()
                .name("Test Election for Voters")
                .description("Test")
                .startTs(now.plus(1, ChronoUnit.DAYS))
                .endTs(now.plus(2, ChronoUnit.DAYS))
                .status(ElectionStatus.DRAFT)
                .maxVotesPerVoter(1)
                .allowAbstention(true)
                .requireJustification(false)
                .build();

        testElection = electionRepository.save(testElection);
    }

    @Test
    @DisplayName("Should register a single voter")
    void shouldRegisterVoter() throws Exception {
        // Given
        RegisterVoterRequest request = new RegisterVoterRequest(
                "12345678900",
                "voter@example.com",
                true,
                null
        );

        // When/Then
        mockMvc.perform(post("/api/admin/elections/{electionId}/voters", testElection.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.externalIdMasked").value("****8900"))
                .andExpect(jsonPath("$.emailMasked").value("v****@example.com"))
                .andExpect(jsonPath("$.eligible").value(true));

        // Verify database
        assertThat(voterRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should not register duplicate voter")
    void shouldNotRegisterDuplicateVoter() throws Exception {
        // Given - first registration
        RegisterVoterRequest request = new RegisterVoterRequest(
                "12345678900",
                "voter@example.com",
                true,
                null
        );

        mockMvc.perform(post("/api/admin/elections/{electionId}/voters", testElection.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // When/Then - try to register again
        mockMvc.perform(post("/api/admin/elections/{electionId}/voters", testElection.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("Should get voters list")
    void shouldGetVotersList() throws Exception {
        // Given - register a voter first
        RegisterVoterRequest request = new RegisterVoterRequest(
                "12345678900",
                "voter@example.com",
                true,
                null
        );

        mockMvc.perform(post("/api/admin/elections/{electionId}/voters", testElection.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // When/Then
        mockMvc.perform(get("/api/admin/elections/{electionId}/voters", testElection.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    @DisplayName("Should get voter statistics")
    void shouldGetVoterStatistics() throws Exception {
        // Given - register voters
        RegisterVoterRequest eligible = new RegisterVoterRequest(
                "11111111111",
                "voter1@example.com",
                true,
                null
        );

        RegisterVoterRequest ineligible = new RegisterVoterRequest(
                "22222222222",
                "voter2@example.com",
                false,
                "Test reason"
        );

        mockMvc.perform(post("/api/admin/elections/{electionId}/voters", testElection.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(eligible)));

        mockMvc.perform(post("/api/admin/elections/{electionId}/voters", testElection.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ineligible)));

        // When/Then
        mockMvc.perform(get("/api/admin/elections/{electionId}/voters/stats", testElection.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVoters").value(2))
                .andExpect(jsonPath("$.eligibleVoters").value(1))
                .andExpect(jsonPath("$.ineligibleVoters").value(1));
    }

    @Test
    @DisplayName("Should import voters from CSV")
    void shouldImportVotersFromCsv() throws Exception {
        // Given
        String csvContent = """
                externalId,email,eligible
                11111111111,voter1@example.com,true
                22222222222,voter2@example.com,true
                33333333333,voter3@example.com,false
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "voters.csv",
                "text/csv",
                csvContent.getBytes()
        );

        // When/Then
        mockMvc.perform(multipart("/api/admin/elections/{electionId}/voters/import", testElection.getId())
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProcessed").value(3))
                .andExpect(jsonPath("$.successCount").value(3))
                .andExpect(jsonPath("$.failureCount").value(0));

        // Verify database
        assertThat(voterRepository.count()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should handle CSV import errors gracefully")
    void shouldHandleCsvImportErrors() throws Exception {
        // Given - CSV with invalid data
        String csvContent = """
                externalId,email,eligible
                11111111111,voter1@example.com,true
                ,invalid@example.com,true
                33333333333,voter3@example.com,true
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "voters.csv",
                "text/csv",
                csvContent.getBytes()
        );

        // When/Then
        mockMvc.perform(multipart("/api/admin/elections/{electionId}/voters/import", testElection.getId())
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProcessed").value(3))
                .andExpect(jsonPath("$.successCount").value(2))
                .andExpect(jsonPath("$.failureCount").value(1))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors.length()").value(1));
    }
}
