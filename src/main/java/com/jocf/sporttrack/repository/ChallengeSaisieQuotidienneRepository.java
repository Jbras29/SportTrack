package com.jocf.sporttrack.repository;

import com.jocf.sporttrack.model.Challenge;
import com.jocf.sporttrack.model.ChallengeSaisieQuotidienne;
import com.jocf.sporttrack.model.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface ChallengeSaisieQuotidienneRepository extends JpaRepository<ChallengeSaisieQuotidienne, Long> {

    boolean existsByChallenge_IdAndUtilisateur_IdAndJour(Long challengeId, Long utilisateurId, LocalDate jour);

    Optional<ChallengeSaisieQuotidienne> findByChallengeAndUtilisateurAndJour(
            Challenge challenge, Utilisateur utilisateur, LocalDate jour);
}
