package com.jocf.sporttrack.repository;

import com.jocf.sporttrack.model.Challenge;
import com.jocf.sporttrack.model.ChallengeSaisieQuotidienne;
import com.jocf.sporttrack.model.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ChallengeSaisieQuotidienneRepository extends JpaRepository<ChallengeSaisieQuotidienne, Long> {

    Optional<ChallengeSaisieQuotidienne> findByChallengeAndUtilisateurAndJour(
            Challenge challenge,
            Utilisateur utilisateur,
            LocalDate jour
    );

    List<ChallengeSaisieQuotidienne> findByChallengeAndRealiseTrue(Challenge challenge);

    boolean existsByChallenge_IdAndUtilisateur_IdAndJour(
        Long challengeId,
        Long utilisateurId,
        LocalDate jour
);
}