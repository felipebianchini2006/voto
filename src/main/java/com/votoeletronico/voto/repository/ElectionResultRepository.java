package com.votoeletronico.voto.repository;

import com.votoeletronico.voto.domain.results.ElectionResult;
import com.votoeletronico.voto.domain.results.TallyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ElectionResultRepository extends JpaRepository<ElectionResult, UUID> {

    Optional<ElectionResult> findByElectionId(UUID electionId);

    boolean existsByElectionId(UUID electionId);

    Optional<ElectionResult> findByElectionIdAndStatus(UUID electionId, TallyStatus status);
}
