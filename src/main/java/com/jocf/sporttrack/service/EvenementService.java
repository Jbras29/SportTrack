package com.jocf.sporttrack.service;

import com.jocf.sporttrack.model.Evenement;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.repository.EvenementRepository;
import com.jocf.sporttrack.repository.UtilisateurRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class EvenementService {

    private final EvenementRepository evenementRepository;
    private final UtilisateurRepository utilisateurRepository;

    @Autowired
    public EvenementService(EvenementRepository evenementRepository, UtilisateurRepository utilisateurRepository) {
        this.evenementRepository = evenementRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    // Crée un nouvel événement et associe l'utilisateur comme organisateur
    @Transactional
    public Evenement creerEvenement(Long organisateurId, Evenement nouvelEvenement) {
        // 1. Récupérer l'organisateur
        Utilisateur organisateur = utilisateurRepository.findById(organisateurId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        nouvelEvenement.setOrganisateur(organisateur);

        // 2. Récupérer les vrais objets Utilisateur depuis la base de données
        // pour les amis qui ont été cochés dans le formulaire frontend
        List<Utilisateur> participantsComplets = new ArrayList<>();

        for (Utilisateur participantPartiel : nouvelEvenement.getParticipants()) {
            utilisateurRepository.findById(participantPartiel.getId())
                    .ifPresent(participantsComplets::add);
        }

        // 3. Ajouter l'organisateur lui-même à la liste
        participantsComplets.add(organisateur);

        // 4. Mettre à jour la liste des participants de l'événement
        nouvelEvenement.setParticipants(participantsComplets);

        // 5. Sauvegarder
        return evenementRepository.save(nouvelEvenement);
    }

    // Récupère tous les événements organisés par un utilisateur spécifique
    public List<Evenement> obtenirEvenementsParOrganisateur(Long organisateurId) {
        Utilisateur organisateur = utilisateurRepository.findById(organisateurId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        return evenementRepository.findByOrganisateur(organisateur);
    }
}
