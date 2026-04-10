package com.jocf.sporttrack.service;

import com.jocf.sporttrack.dto.LigneClassementChallenge;
import com.jocf.sporttrack.model.Challenge;
import com.jocf.sporttrack.model.ChallengeSaisieQuotidienne;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.repository.ChallengeRepository;
import com.jocf.sporttrack.repository.ChallengeSaisieQuotidienneRepository;
import com.jocf.sporttrack.repository.UtilisateurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ChallengeService {

    private final ChallengeRepository challengeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ChallengeSaisieQuotidienneRepository challengeSaisieQuotidienneRepository;

    public ChallengeService(
            ChallengeRepository challengeRepository,
            UtilisateurRepository utilisateurRepository,
            ChallengeSaisieQuotidienneRepository challengeSaisieQuotidienneRepository) {
        this.challengeRepository = challengeRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.challengeSaisieQuotidienneRepository = challengeSaisieQuotidienneRepository;
    }

    public List<Challenge> recupererTousLesChallenges() {
        return challengeRepository.findAll();
    }

    public Optional<Challenge> trouverParId(Long id) {
        return challengeRepository.findById(id);
    }

    public Challenge creerChallenge(Challenge challenge, Long organisateurId) {
        Utilisateur organisateur = utilisateurRepository.findById(organisateurId)
                .orElseThrow(() -> new IllegalArgumentException("Organisateur introuvable : " + organisateurId));

        if (challenge.getDateFin() != null && challenge.getDateDebut() != null
                && challenge.getDateFin().before(challenge.getDateDebut())) {
            throw new IllegalArgumentException("La date de fin doit être après la date de début.");
        }

        challenge.setId(null);
        challenge.setOrganisateur(organisateur);
        return challengeRepository.save(challenge);
    }

    public Challenge modifierChallenge(Long id, Challenge challengeDetails) {
        Challenge challenge = challengeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Challenge introuvable : " + id));

        challenge.setNom(challengeDetails.getNom());
        challenge.setDateDebut(challengeDetails.getDateDebut());
        challenge.setDateFin(challengeDetails.getDateFin());

        return challengeRepository.save(challenge);
    }

    public void supprimerChallenge(Long id) {
        if (!challengeRepository.existsById(id)) {
            throw new IllegalArgumentException("Challenge introuvable : " + id);
        }
        challengeRepository.deleteById(id);
    }

    public List<LigneClassementChallenge> getClassement(Long challengeId) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new IllegalArgumentException("Challenge introuvable : " + challengeId));

        List<ChallengeSaisieQuotidienne> saisiesValidees =
                challengeSaisieQuotidienneRepository.findByChallengeAndRealiseTrue(challenge);

        Map<Long, Long> scoresParUtilisateurId = saisiesValidees.stream()
                .collect(Collectors.groupingBy(
                        saisie -> saisie.getUtilisateur().getId(),
                        Collectors.counting()
                ));

        return challenge.getParticipants().stream()
                .map(utilisateur -> new LigneClassementChallenge(
                        utilisateur,
                        scoresParUtilisateurId.getOrDefault(utilisateur.getId(), 0L)
                ))
                .sorted(Comparator.comparingLong(LigneClassementChallenge::getScore).reversed())
                .collect(Collectors.toList());
    }

    public Challenge rejoindreChallenge(Long challengeId, Long utilisateurId) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new IllegalArgumentException("Challenge introuvable : " + challengeId));

        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable : " + utilisateurId));

        if (challenge.getDateFin() != null
                && challenge.getDateFin().before(new java.sql.Date(System.currentTimeMillis()))) {
            throw new IllegalArgumentException("Ce challenge est terminé.");
        }

        if (challenge.getParticipants().contains(utilisateur)) {
            throw new IllegalArgumentException("Vous participez déjà à ce challenge.");
        }

        challenge.getParticipants().add(utilisateur);
        return challengeRepository.save(challenge);
    }

    @Transactional
    public void enregistrerSaisieQuotidienne(Long challengeId, Long utilisateurId, LocalDate jour, boolean realise) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new IllegalArgumentException("Challenge introuvable : " + challengeId));

        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable : " + utilisateurId));

        if (!challenge.getParticipants().contains(utilisateur)) {
            throw new IllegalArgumentException("Vous ne participez pas à ce challenge.");
        }

        Optional<ChallengeSaisieQuotidienne> existant =
                challengeSaisieQuotidienneRepository.findByChallengeAndUtilisateurAndJour(challenge, utilisateur, jour);

        if (existant.isPresent()) {
            ChallengeSaisieQuotidienne saisie = existant.get();
            saisie.setRealise(realise);
            challengeSaisieQuotidienneRepository.save(saisie);
        } else {
            challengeSaisieQuotidienneRepository.save(
                    ChallengeSaisieQuotidienne.builder()
                            .challenge(challenge)
                            .utilisateur(utilisateur)
                            .jour(jour)
                            .realise(realise)
                            .build()
            );
        }
    }

    public Boolean recupererReponseDuJour(Long challengeId, Long utilisateurId, LocalDate jour) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new IllegalArgumentException("Challenge introuvable : " + challengeId));
    
        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable : " + utilisateurId));
    
        return challengeSaisieQuotidienneRepository
                .findByChallengeAndUtilisateurAndJour(challenge, utilisateur, jour)
                .map(ChallengeSaisieQuotidienne::isRealise)
                .orElse(null);
    }

    @Transactional
public void supprimerChallengeSiOrganisateur(Long challengeId, Long utilisateurId) {
    Challenge challenge = challengeRepository.findById(challengeId)
            .orElseThrow(() -> new IllegalArgumentException("Challenge introuvable : " + challengeId));

    if (challenge.getOrganisateur() == null || !challenge.getOrganisateur().getId().equals(utilisateurId)) {
        throw new IllegalArgumentException("Seul l'organisateur peut supprimer ce challenge.");
    }

    challengeSaisieQuotidienneRepository.deleteByChallenge(challenge);
    challengeRepository.delete(challenge);
}
}