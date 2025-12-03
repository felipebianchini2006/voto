package com.votoeletronico.voto.controller.voting;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.votoeletronico.voto.BaseIntegrationTest;
import com.votoeletronico.voto.domain.election.Candidate;
import com.votoeletronico.voto.domain.election.Election;
import com.votoeletronico.voto.domain.election.ElectionStatus;
import com.votoeletronico.voto.domain.voter.Voter;
import com.votoeletronico.voto.dto.request.CastAbstentionRequest;
import com.votoeletronico.voto.dto.request.CastVoteRequest;
import com.votoeletronico.voto.dto.request.TokenRequest;
import com.votoeletronico.voto.repository.CandidateRepository;
import com.votoeletronico.voto.repository.ElectionRepository;
import com.votoeletronico.voto.repository.EncryptedBallotRepository;
import com.votoeletronico.voto.repository.VoterRepository;
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

@DisplayName("VotingController Integration Tests")
class VotingControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ElectionRepository electionRepository;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private VoterRepository voterRepository;

    @Autowired
    private EncryptedBallotRepository ballotRepository;

    private Election testElection;
    private Candidate testCandidate;
    private Voter testVoter;

    @BeforeEach
    void setUp() {
        ballotRepository.deleteAll();
        voterRepository.deleteAll();
        candidateRepository.deleteAll();
        electionRepository.deleteAll();

        // Create running election
        Instant now = Instant.now();
        testElection = Election.builder()
                .name("Test Election for Voting")
                .description("Test")
                .startTs(now.minus(1, ChronoUnit.HOURS))
                .endTs(now.plus(2, ChronoUnit.HOURS))
                .status(ElectionStatus.RUNNING)
                .maxVotesPerVoter(1)
                .allowAbstention(true)
                .requireJustification(false)
                .build();
        testElection = electionRepository.save(testElection);

        // Create candidate
        testCandidate = Candidate.builder()
                .election(testElection)
                .name("Test Candidate")
                .ballotNumber(10)
                .party("Test Party")
                .build();
        testCandidate = candidateRepository.save(testCandidate);

        // Create eligible voter
        testVoter = new Voter();
        testVoter.setElection(testElection);
        testVoter.setExternalId("12345678900");
        testVoter.setEmail("voter@test.com");
        testVoter.setEligible(true);
        testVoter = voterRepository.save(testVoter);
    }

    @Test
    @DisplayName("Should request token successfully")
    void shouldRequestToken() throws Exception {
        // Given
        TokenRequest request = new TokenRequest("12345678900");

        // When/Then
        mockMvc.perform(post("/api/voting/elections/{electionId}/token", testElection.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.electionId").value(testElection.getId().toString()))
                .andExpect(jsonPath("$.status").value("ISSUED"))
                .andExpect(jsonPath("$.tokenValue").exists())
                .andExpect(jsonPath("$.signature").exists())
                .andExpect(jsonPath("$.publicKey").exists());
    }

    @Test
    @DisplayName("Should not issue token to ineligible voter")
    void shouldNotIssueTokenToIneligibleVoter() throws Exception {
        // Given - make voter ineligible
        testVoter.markAsIneligible("Test reason");
        voterRepository.save(testVoter);

        TokenRequest request = new TokenRequest("12345678900");

        // When/Then
        mockMvc.perform(post("/api/voting/elections/{electionId}/token", testElection.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("Should not issue duplicate token")
    void shouldNotIssueDuplicateToken() throws Exception {
        // Given - request first token
        TokenRequest request = new TokenRequest("12345678900");

        mockMvc.perform(post("/api/voting/elections/{electionId}/token", testElection.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // When/Then - try to request another token
        mockMvc.perform(post("/api/voting/elections/{electionId}/token", testElection.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("Should cast vote successfully")
    void shouldCastVote() throws Exception {
        // Given - get token first
        TokenRequest tokenRequest = new TokenRequest("12345678900");

        String tokenResponse = mockMvc.perform(post("/api/voting/elections/{electionId}/token", testElection.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tokenRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String tokenValue = objectMapper.readTree(tokenResponse).get("tokenValue").asText();

        // When/Then - cast vote
        CastVoteRequest voteRequest = new CastVoteRequest(tokenValue, testCandidate.getId());

        mockMvc.perform(post("/api/voting/elections/{electionId}/vote", testElection.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(voteRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ballotId").exists())
                .andExpect(jsonPath("$.ballotHash").exists())
                .andExpect(jsonPath("$.castAt").exists())
                .andExpect(jsonPath("$.message").exists());

        // Verify ballot was saved
        assertThat(ballotRepository.countByElectionId(testElection.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("Should cast abstention successfully")
    void shouldCastAbstention() throws Exception {
        // Given - get token first
        TokenRequest tokenRequest = new TokenRequest("12345678900");

        String tokenResponse = mockMvc.perform(post("/api/voting/elections/{electionId}/token", testElection.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tokenRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String tokenValue = objectMapper.readTree(tokenResponse).get("tokenValue").asText();

        // When/Then - cast abstention
        CastAbstentionRequest abstentionRequest = new CastAbstentionRequest(tokenValue, "Personal reasons");

        mockMvc.perform(post("/api/voting/elections/{electionId}/abstain", testElection.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(abstentionRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ballotId").exists())
                .andExpect(jsonPath("$.ballotHash").exists());

        // Verify ballot was saved
        assertThat(ballotRepository.countByElectionId(testElection.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("Should not allow double voting with same token")
    void shouldNotAllowDoubleVoting() throws Exception {
        // Given - get token and cast vote
        TokenRequest tokenRequest = new TokenRequest("12345678900");

        String tokenResponse = mockMvc.perform(post("/api/voting/elections/{electionId}/token", testElection.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tokenRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String tokenValue = objectMapper.readTree(tokenResponse).get("tokenValue").asText();

        CastVoteRequest voteRequest = new CastVoteRequest(tokenValue, testCandidate.getId());

        mockMvc.perform(post("/api/voting/elections/{electionId}/vote", testElection.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(voteRequest)))
                .andExpect(status().isCreated());

        // When/Then - try to vote again with same token
        mockMvc.perform(post("/api/voting/elections/{electionId}/vote", testElection.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(voteRequest)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("Should verify ballot receipt")
    void shouldVerifyBallotReceipt() throws Exception {
        // Given - cast a vote first
        TokenRequest tokenRequest = new TokenRequest("12345678900");

        String tokenResponse = mockMvc.perform(post("/api/voting/elections/{electionId}/token", testElection.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tokenRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String tokenValue = objectMapper.readTree(tokenResponse).get("tokenValue").asText();

        CastVoteRequest voteRequest = new CastVoteRequest(tokenValue, testCandidate.getId());

        String voteResponse = mockMvc.perform(post("/api/voting/elections/{electionId}/vote", testElection.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(voteRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String ballotHash = objectMapper.readTree(voteResponse).get("ballotHash").asText();

        // When/Then - verify ballot
        mockMvc.perform(get("/api/voting/elections/{electionId}/verify/{ballotHash}",
                        testElection.getId(), ballotHash))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true))
                .andExpect(jsonPath("$.castAt").exists());
    }

    @Test
    @DisplayName("Should return not found for invalid ballot hash")
    void shouldReturnNotFoundForInvalidBallotHash() throws Exception {
        // When/Then
        mockMvc.perform(get("/api/voting/elections/{electionId}/verify/{ballotHash}",
                        testElection.getId(), "invalid-hash"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(false));
    }

    @Test
    @DisplayName("Should get voting statistics")
    void shouldGetVotingStatistics() throws Exception {
        // Given - cast one vote
        TokenRequest tokenRequest = new TokenRequest("12345678900");

        String tokenResponse = mockMvc.perform(post("/api/voting/elections/{electionId}/token", testElection.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tokenRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String tokenValue = objectMapper.readTree(tokenResponse).get("tokenValue").asText();

        CastVoteRequest voteRequest = new CastVoteRequest(tokenValue, testCandidate.getId());

        mockMvc.perform(post("/api/voting/elections/{electionId}/vote", testElection.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(voteRequest)))
                .andExpect(status().isCreated());

        // When/Then - get stats
        mockMvc.perform(get("/api/voting/elections/{electionId}/stats", testElection.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokensIssued").value(1))
                .andExpect(jsonPath("$.tokensConsumed").value(1))
                .andExpect(jsonPath("$.totalBallots").value(1))
                .andExpect(jsonPath("$.turnoutPercentage").value(100.0));
    }

    @Test
    @DisplayName("Should verify ballot chain integrity")
    void shouldVerifyBallotChainIntegrity() throws Exception {
        // Given - cast multiple votes to create chain
        for (int i = 0; i < 3; i++) {
            String externalId = "1234567890" + i;

            // Create voter
            Voter voter = new Voter();
            voter.setElection(testElection);
            voter.setExternalId(externalId);
            voter.setEmail("voter" + i + "@test.com");
            voter.setEligible(true);
            voterRepository.save(voter);

            // Get token
            TokenRequest tokenRequest = new TokenRequest(externalId);

            String tokenResponse = mockMvc.perform(post("/api/voting/elections/{electionId}/token", testElection.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(tokenRequest)))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();

            String tokenValue = objectMapper.readTree(tokenResponse).get("tokenValue").asText();

            // Cast vote
            CastVoteRequest voteRequest = new CastVoteRequest(tokenValue, testCandidate.getId());

            mockMvc.perform(post("/api/voting/elections/{electionId}/vote", testElection.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(voteRequest)))
                    .andExpect(status().isCreated());
        }

        // When/Then - verify chain
        mockMvc.perform(get("/api/voting/elections/{electionId}/verify-chain", testElection.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chainValid").value(true));
    }
}
