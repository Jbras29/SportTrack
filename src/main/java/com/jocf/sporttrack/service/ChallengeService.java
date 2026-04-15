package com.jocf.sporttrack.service;

import com.jocf.sporttrack.dto.CreerChallengeRequest;
import com.jocf.sporttrack.dto.LigneClassementChallenge;
import com.jocf.sporttrack.dto.ModifierChallengeRequest;
import com.jocf.sporttrack.model.Challenge;
import com.jocf.sporttrack.model.ChallengeSaisieQuotidienne;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.repository.ChallengeRepository;
import com.jocf.sporttrack.repository.ChallengeSaisieQuotidienneRepository;
import com.jocf.sporttrack.repository.UtilisateurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ChallengeService {

    private static final String MSG_CHALLENGE_INTROUVABLE = "Challenge introuvable : ";
    private static final String MSG_UTILISATEUR_INTROUVABLE = "Utilisateur introuvable : ";
    private static final String MSG_JA_DEJA_REPONDU = "Vous avez déjà répondu pour ce jour.";

    private final ChallengeRepository challengeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ChallengeSaisieQuotidienneRepository challengeSaisieQuotidienneRepository;
    private final UtilisateurService utilisateurService;

    public ChallengeService(
            ChallengeRepository challengeRepository,
            UtilisateurRepository utilisateurRepository,
            ChallengeSaisieQuotidienneRepository challengeSaisieQuotidienneRepository,
            UtilisateurService utilisateurService) {
        this.challengeRepository = challengeRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.challengeSaisieQuotidienneRepository = challengeSaisieQuotidienneRepository;
        this.utilisateurService = utilisateurService;
    }

    public List<Challenge> recupererTousLesChallenges() {
        return challengeRepository.findAll();
    }

    public Optional<Challenge> trouverParId(Long id) {
        return challengeRepository.findById(id);
    }

    public Challenge creerChallenge(CreerChallengeRequest req, Long organisateurId) {
        Utilisateur organisateur = utilisateurRepository.findById(organisateurId)
                .orElseThrow(() -> new IllegalArgumentException("Organisateur introuvable : " + organisateurId));

        Date dateDebut = Date.valueOf(req.dateDebut());
        Date dateFin = req.dateFin() != null ? Date.valueOf(req.dateFin()) : null;

        if (dateFin != null && dateFin.before(dateDebut)) {
            throw new IllegalArgumentException("La date de fin doit être après la date de début.");
        }

        Challenge challenge = Challenge.builder()
                .nom(req.nom())
                .dateDebut(dateDebut)
                .dateFin(dateFin)
                .build();
        challenge.setOrganisateur(organisateur);
        return challengeRepository.save(challenge);
    }

    public Challenge modifierChallenge(Long id, ModifierChallengeRequest req) {
        Challenge challenge = challengeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(MSG_CHALLENGE_INTROUVABLE + id));

        challenge.setNom(req.nom());
        if (req.dateDebut() != null) {
            challenge.setDateDebut(Date.valueOf(req.dateDebut()));
        }
        if (req.dateFin() != null) {
            challenge.setDateFin(Date.valueOf(req.dateFin()));
        }

        return challengeRepository.save(challenge);
    }

    public void supprimerChallenge(Long id) {
        if (!challengeRepository.existsById(id)) {
            throw new IllegalArgumentException(MSG_CHALLENGE_INTROUVABLE + id);
        }
        challengeRepository.deleteById(id);
    }

    public List<LigneClassementChallenge> getClassement(Long challengeId) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new IllegalArgumentException(MSG_CHALLENGE_INTROUVABLE + challengeId));

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
                .toList();
    }

    public Challenge rejoindreChallenge(Long challengeId, Long utilisateurId) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new IllegalArgumentException(MSG_CHALLENGE_INTROUVABLE + challengeId));

        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new IllegalArgumentException(MSG_UTILISATEUR_INTROUVABLE + utilisateurId));

        if (utilisateur.getHpNormalise() <= 0) {
            throw new IllegalArgumentException("Action impossible : votre barre de vie est à 0.");
        }

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
        enregistrerSaisieQuotidienne(challengeId, utilisateurId, jour, realise, false);
    }

    @Transactional
    public void enregistrerAbsenceQuotidienne(Long challengeId, Long utilisateurId, LocalDate jour) {
        enregistrerSaisieQuotidienne(challengeId, utilisateurId, jour, false, true);
    }

    public boolean aDejaReponduLeJour(Long challengeId, Long utilisateurId, LocalDate jour) {
        return challengeSaisieQuotidienneRepository.existsByChallenge_IdAndUtilisateur_IdAndJour(
                challengeId, utilisateurId, jour);
    }

    @Transactional
    public void sanctionnerAbsencesQuotidiennes(LocalDate jour) {
        for (Challenge challenge : challengeRepository.findAll()) {
            if (!challengeActifPourDate(challenge, jour)) {
                continue;
            }

            for (Utilisateur participant : challenge.getParticipants()) {
                if (!aDejaReponduLeJour(challenge.getId(), participant.getId(), jour)) {
                    enregistrerAbsenceQuotidienne(challenge.getId(), participant.getId(), jour);
                }
            }
        }
    }

    private void enregistrerSaisieQuotidienne(
            Long challengeId,
            Long utilisateurId,
            LocalDate jour,
            boolean realise,
            boolean saisieAutomatique) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new IllegalArgumentException(MSG_CHALLENGE_INTROUVABLE + challengeId));

        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new IllegalArgumentException(MSG_UTILISATEUR_INTROUVABLE + utilisateurId));

        if (!challenge.getParticipants().contains(utilisateur)) {
            throw new IllegalArgumentException("Vous ne participez pas à ce challenge.");
        }

        if (challengeSaisieQuotidienneRepository.existsByChallenge_IdAndUtilisateur_IdAndJour(
                challengeId, utilisateurId, jour)) {
            throw new IllegalArgumentException(MSG_JA_DEJA_REPONDU);
        }

        challengeSaisieQuotidienneRepository.save(
                ChallengeSaisieQuotidienne.builder()
                        .challenge(challenge)
                        .utilisateur(utilisateur)
                        .jour(jour)
                        .realise(realise)
                        .saisieAutomatique(saisieAutomatique)
                        .build()
        );

        if (!realise) {
            utilisateurService.appliquerPunitionChallenge(utilisateurId, saisieAutomatique ? 2 : 1);
        }
    }

    private static boolean challengeActifPourDate(Challenge challenge, LocalDate jour) {
        if (challenge.getDateDebut() != null && jour.isBefore(challenge.getDateDebut().toLocalDate())) {
            return false;
        }
        return challenge.getDateFin() == null || !jour.isAfter(challenge.getDateFin().toLocalDate());
    }

    public Boolean recupererReponseDuJour(Long challengeId, Long utilisateurId, LocalDate jour) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new IllegalArgumentException(MSG_CHALLENGE_INTROUVABLE + challengeId));

        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new IllegalArgumentException(MSG_UTILISATEUR_INTROUVABLE + utilisateurId));

        return challengeSaisieQuotidienneRepository
                .findByChallengeAndUtilisateurAndJour(challenge, utilisateur, jour)
                .map(ChallengeSaisieQuotidienne::isRealise)
                .orElse(null);
    }

    @Transactional
    public void supprimerChallengeSiOrganisateur(Long challengeId, Long utilisateurId) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new IllegalArgumentException(MSG_CHALLENGE_INTROUVABLE + challengeId));

        if (challenge.getOrganisateur() == null || !challenge.getOrganisateur().getId().equals(utilisateurId)) {
            throw new IllegalArgumentException("Seul l'organisateur peut supprimer ce challenge.");
        }

        challengeSaisieQuotidienneRepository.deleteByChallenge(challenge);
        challengeRepository.delete(challenge);
    }

    public List<Challenge> trouverChallengesTerminesLe(java.time.LocalDate date) {
        java.sql.Date sqlDate = java.sql.Date.valueOf(date);
        return challengeRepository.findByDateFin(sqlDate);
    }
}
