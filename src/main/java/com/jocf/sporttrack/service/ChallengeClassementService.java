package com.jocf.sporttrack.service;

import com.jocf.sporttrack.dto.LigneClassementChallenge;
import com.jocf.sporttrack.model.Challenge;
import com.jocf.sporttrack.model.ChallengeSaisieQuotidienne;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.repository.ChallengeSaisieQuotidienneRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChallengeClassementService {

    private final ChallengeSaisieQuotidienneRepository saisieRepository;

    public ChallengeClassementService(ChallengeSaisieQuotidienneRepository saisieRepository) {
        this.saisieRepository = saisieRepository;
    }

    public List<LigneClassementChallenge> getClassementPourChallenge(Challenge challenge) {
        List<ChallengeSaisieQuotidienne> saisiesValidees =
                saisieRepository.findByChallengeAndRealiseTrue(challenge);

        Map<Utilisateur, Long> scores = saisiesValidees.stream()
                .collect(Collectors.groupingBy(
                        ChallengeSaisieQuotidienne::getUtilisateur,
                        Collectors.counting()
                ));

        return challenge.getParticipants().stream()
                .map(utilisateur -> new LigneClassementChallenge(
                        utilisateur,
                        scores.getOrDefault(utilisateur, 0L)
                ))
                .sorted(Comparator.comparingLong(LigneClassementChallenge::getScore).reversed())
                .toList();
    }
}