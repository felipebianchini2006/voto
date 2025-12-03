package com.votoeletronico.voto.repository;

import com.votoeletronico.voto.domain.results.CandidateResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CandidateResultRepository extends JpaRepository<CandidateResult, UUID> {

    List<CandidateResult> findByElectionResultId(UUID electionResultId);

    List<CandidateResult> findByElectionResultIdOrderByVoteCountDesc(UUID electionResultId);
}
