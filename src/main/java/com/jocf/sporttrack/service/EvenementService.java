package com.jocf.sporttrack.service;

import com.jocf.sporttrack.dto.CreerEvenementRequest;
import com.jocf.sporttrack.dto.ModifierEvenementRequest;
import com.jocf.sporttrack.model.Annonce;
import com.jocf.sporttrack.model.Evenement;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.repository.AnnonceRepository;
import com.jocf.sporttrack.repository.EvenementRepository;
import com.jocf.sporttrack.repository.UtilisateurRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class EvenementService {

    private final EvenementService self;
    private final EvenementRepository evenementRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AnnonceRepository annonceRepository;

    public EvenementService(
            @Lazy EvenementService self,
            EvenementRepository evenementRepository,
            UtilisateurRepository utilisateurRepository,
            AnnonceRepository annonceRepository) {
        this.self = self;
        this.evenementRepository = evenementRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.annonceRepository = annonceRepository;
    }

    // Crée un nouvel événement et associe l'utilisateur comme organisateur
    @Transactional
    public Evenement creerEvenement(Long organisateurId, CreerEvenementRequest req) {
        Utilisateur organisateur = utilisateurRepository.findById(organisateurId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));

        List<Utilisateur> participantsComplets = new ArrayList<>();
        for (CreerEvenementRequest.ParticipantIdRef p : req.participants()) {
            if (p.id() != null) {
                utilisateurRepository.findById(p.id()).ifPresent(participantsComplets::add);
            }
        }
        participantsComplets.add(organisateur);

        String description = req.description() != null && !req.description().isBlank()
                ? req.description()
                : "";

        Evenement nouvelEvenement = Evenement.builder()
                .nom(req.nom())
                .description(description)
                .date(req.date())
                .organisateur(organisateur)
                .participants(participantsComplets)
                .build();

        return evenementRepository.save(nouvelEvenement);
    }

    // Récupère tous les événements organisés par un utilisateur spécifique
    public List<Evenement> obtenirEvenementsParOrganisateur(Long organisateurId) {
        Utilisateur organisateur = utilisateurRepository.findById(organisateurId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));

        return evenementRepository.findByOrganisateur(organisateur);
    }




    @Transactional
    public Evenement rejoindreEvenement(Long evenementId, Utilisateur utilisateur) {
        // 1. Trouver l'événement par son ID
        Evenement evenement = evenementRepository.findById(evenementId)
                .orElseThrow(() -> new IllegalArgumentException("Événement introuvable"));

        // 2. Vérifier si l'utilisateur ne participe pas déjà
        if (evenement.getParticipants().contains(utilisateur)) {
            throw new IllegalArgumentException("Vous participez déjà à cet événement");
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
                .orElseThrow(() -> new IllegalArgumentException("Événement introuvable avec l'ID : " + id));
    }

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
            throw new IllegalArgumentException("Annonce introuvable");
        }
    }

    // 2. Modifier l'événement
    @Transactional
    public Evenement modifierEvenement(Long id, ModifierEvenementRequest req) {
        Evenement evenement = trouverParId(id);

        evenement.setNom(req.nom());
        evenement.setDate(req.date());
        evenement.setDescription(req.description());

        return evenementRepository.save(evenement);
    }

    /** Retirer un participant de la liste des participants d'un événement. */
    @Transactional
    public void retirerParticipant(Long evenementId, Long utilisateurId) {
        // 1. Trouver l'événement
        Evenement evenement = trouverParId(evenementId);

        // 2. Retirer l'utilisateur de la liste par son ID
        // removeIf est très efficace ici
        evenement.getParticipants().removeIf(p -> p.getId().equals(utilisateurId));

        // 3. Sauvegarder les modifications
        evenementRepository.save(evenement);
    }

    /** Supprimer l'utilisateur actuel de la liste des participants. */
    @Transactional
    public void quitterEvenement(Long evenementId, Long utilisateurId) {
        self.retirerParticipant(evenementId, utilisateurId);
    }

    /** 🚀 Supprimer un événement par son ID. */
    @Transactional
    public void supprimer(Long id) {
        // En JPA, la suppression simple suffit si les cascades sont bien configurées
        evenementRepository.deleteById(id);
    }
}
