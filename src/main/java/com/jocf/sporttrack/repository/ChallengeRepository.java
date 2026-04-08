package com.jocf.sporttrack.repository;

import com.jocf.sporttrack.model.Challenge;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChallengeRepository extends JpaRepository<Challenge, Long> {

    List<Challenge> findByParticipants_IdOrderByDateFinAsc(Long utilisateurId);
}
