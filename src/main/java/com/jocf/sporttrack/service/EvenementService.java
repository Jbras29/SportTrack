package com.jocf.sporttrack.service;

import com.jocf.sporttrack.model.Annonce;
import com.jocf.sporttrack.model.Evenement;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.repository.AnnonceRepository;
import com.jocf.sporttrack.repository.EvenementRepository;
import com.jocf.sporttrack.repository.UtilisateurRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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




    @Transactional
    public Evenement rejoindreEvenement(Long evenementId, Utilisateur utilisateur) {
        // 1. Trouver l'événement par son ID
        Evenement evenement = evenementRepository.findById(evenementId)
                .orElseThrow(() -> new RuntimeException("Événement introuvable"));

        // 2. Vérifier si l'utilisateur ne participe pas déjà
        if (evenement.getParticipants().contains(utilisateur)) {
            throw new RuntimeException("Vous participez déjà à cet événement");
        }

        // 3. Ajouter l'utilisateur à la liste des participants
        evenement.getParticipants().add(utilisateur);

        // 4. Sauvegarder et retourner l'événement mis à jour
        return evenementRepository.save(evenement);
    }

    // Récupérer la liste complète de tous les événements
    public List<Evenement> obtenirTousLesEvenements() {
        return evenementRepository.findAll(); // Méthode fournie automatiquement par Spring Data JPA
    }

    public Evenement trouverParId(Long id) {
        return evenementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Événement introuvable avec l'ID : " + id));
    }


    // ... autres injections (EvenementRepository, etc.)
    @Autowired
    private AnnonceRepository annonceRepository;

    // 1. Publier une annonce
    @Transactional
    public Annonce ajouterAnnonce(Long evenementId, String message) {
        Evenement evenement = trouverParId(evenementId);

        // Utilisation du Builder de ton entité Annonce
        Annonce annonce = Annonce.builder()
                .message(message)
                .date(LocalDateTime.now()) // Définit l'heure actuelle
                .evenement(evenement)
                .build();

        return annonceRepository.save(annonce);
    }

    // Supprimer une annonce spécifique par son ID
    @Transactional
    public void supprimerAnnonce(Long annonceId) {
        // On vérifie d'abord si l'annonce existe avant de supprimer
        if (annonceRepository.existsById(annonceId)) {
            annonceRepository.deleteById(annonceId);
        } else {
            throw new RuntimeException("Annonce introuvable");
        }
    }

    // 2. Modifier l'événement
    @Transactional
    public Evenement modifierEvenement(Long id, Evenement nouveauxDetails) {
        Evenement evenement = trouverParId(id);

        evenement.setNom(nouveauxDetails.getNom());
        evenement.setDate(nouveauxDetails.getDate());
        evenement.setDescription(nouveauxDetails.getDescription());

        return evenementRepository.save(evenement);
    }
}
