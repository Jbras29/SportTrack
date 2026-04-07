package com.jocf.sporttrack.service;

import com.jocf.sporttrack.model.Challenge;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.repository.ChallengeRepository;
import com.jocf.sporttrack.repository.UtilisateurRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ChallengeService {

    private final ChallengeRepository challengeRepository;
    private final UtilisateurRepository utilisateurRepository;

    public ChallengeService(ChallengeRepository challengeRepository, UtilisateurRepository utilisateurRepository) {
        this.challengeRepository = challengeRepository;
        this.utilisateurRepository = utilisateurRepository;
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
}