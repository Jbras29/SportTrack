package com.jocf.sporttrack.controller;

import com.jocf.sporttrack.model.Evenement;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.service.EvenementService;
import com.jocf.sporttrack.service.UtilisateurService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// On utilise @Controller au lieu de @RestController pour pouvoir renvoyer des pages HTML
@Controller
public class EvenementController {

    private final EvenementService evenementService;
    private final UtilisateurService utilisateurService;

    @Autowired
    public EvenementController(EvenementService evenementService, UtilisateurService utilisateurService) {
        this.evenementService = evenementService;
        this.utilisateurService = utilisateurService;
    }

    // =======================================================
    // SECTION 1 : VUES (Redirection vers les pages HTML + Thymeleaf)
    // =======================================================

    @GetMapping("/evenements")
    public String afficherPageEvenements(Model model) {
        List<Evenement> listeEvenements = evenementService.obtenirTousLesEvenements();
        model.addAttribute("evenements", listeEvenements);

        return "evenement/evenement";
    }

    @GetMapping("/creer-evenement")
    public String afficherPageCreerEvenement(Model model) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String email = (principal instanceof UserDetails) ? ((UserDetails) principal).getUsername() : principal.toString();
        Utilisateur currentUser = utilisateurService.trouverParEmail(email);

        model.addAttribute("amis", currentUser.getAmis());

        return "evenement/creer-evenement";
    }


    // Afficher la page de détail d'un événement
    @GetMapping("/evenements/{id}")
    public String afficherDetailEvenement(@PathVariable Long id, Model model) {
        Evenement evenement = evenementService.trouverParId(id);

        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String email = (principal instanceof UserDetails) ? ((UserDetails) principal).getUsername() : principal.toString();
        Utilisateur currentUser = utilisateurService.trouverParEmail(email);

        boolean isOrganisateur = evenement.getOrganisateur().getId().equals(currentUser.getId());

        model.addAttribute("evenement", evenement);
        model.addAttribute("isOrganisateur", isOrganisateur);
        model.addAttribute("currentUser", currentUser);

        return "evenement/detail";
    }


    // =======================================================
    // SECTION 2 : API REST (Traitement des données JSON)
    // =======================================================

    // Endpoint pour récupérer tous les événements
    @ResponseBody // Indique que cette méthode renvoie du JSON, pas une page HTML
    @GetMapping("/api/evenements")
    @Operation(summary = "Récupérer tous les événements")
    public ResponseEntity<List<Evenement>> getAllEvenements() {
        List<Evenement> evenements = evenementService.obtenirTousLesEvenements();
        return ResponseEntity.ok(evenements);
    }

    // Endpoint pour créer un nouvel événement
    @ResponseBody
    @PostMapping("/api/evenements/creer")
    @Operation(summary = "Créer un nouvel événement")
    public ResponseEntity<Evenement> creerEvenement(@RequestBody Evenement evenement) {
        // 1. Récupérer l'email de l'utilisateur connecté via Spring Security
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String email;
        if (principal instanceof UserDetails) {
            email = ((UserDetails) principal).getUsername();
        } else {
            email = principal.toString();
        }

        // 2. Trouver l'utilisateur dans la base de données
        Utilisateur organisateur = utilisateurService.trouverParEmail(email);

        // 3. Créer l'événement en utilisant son ID
        Evenement evenementCree = evenementService.creerEvenement(organisateur.getId(), evenement);

        return new ResponseEntity<>(evenementCree, HttpStatus.CREATED);
    }

    // Endpoint pour rejoindre un événement existant
    @ResponseBody
    @PostMapping("/api/evenements/{id}/rejoindre")
    @Operation(summary = "Rejoindre un événement existant")
    public ResponseEntity<?> rejoindreEvenement(@PathVariable Long id) {
        try {
            // Obtenir l'email de l'utilisateur connecté
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            String email;
            if (principal instanceof UserDetails) {
                email = ((UserDetails) principal).getUsername();
            } else {
                email = principal.toString();
            }

            // Trouver l'utilisateur et appeler le service
            Utilisateur utilisateurCourant = utilisateurService.trouverParEmail(email);
            Evenement evenementMisAJour = evenementService.rejoindreEvenement(id, utilisateurCourant);

            return ResponseEntity.ok(evenementMisAJour);

        } catch (RuntimeException e) {
            // En cas d'erreur (ex: déjà participant)
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Endpoint pour récupérer les événements d'un organisateur
    @ResponseBody
    @GetMapping("/api/evenements/organisateur/{organisateurId}")
    public ResponseEntity<List<Evenement>> getEvenementsByOrganisateur(@PathVariable Long organisateurId) {
        List<Evenement> evenements = evenementService.obtenirEvenementsParOrganisateur(organisateurId);
        return ResponseEntity.ok(evenements);
    }


    // Modifier les informations d'un événement
    @ResponseBody
    @PutMapping("/api/evenements/{id}")
    public ResponseEntity<?> modifierEvenement(@PathVariable Long id, @RequestBody Evenement nouveauxDetails) {
        // Sécurité : Vérifier si c'est bien l'organisateur qui demande la modif
        if (verifierSiOrganisateur(id)) {
            // On appelle le service pour mettre à jour
            return ResponseEntity.ok(evenementService.modifierEvenement(id, nouveauxDetails));
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Action non autorisée");
    }

    // Publier une annonce
    @ResponseBody
    @PostMapping("/api/evenements/{id}/annonces")
    public ResponseEntity<?> creerAnnonce(@PathVariable Long id, @RequestBody java.util.Map<String, String> payload) {
        if (!verifierSiOrganisateur(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Non autorisé");
        }

        // On récupère le champ "message" envoyé par le front
        String message = payload.get("message");
        return ResponseEntity.ok(evenementService.ajouterAnnonce(id, message));
    }

    // Endpoint pour supprimer une annonce
    @ResponseBody
    @DeleteMapping("/api/evenements/{id}/annonces/{annonceId}")
    public ResponseEntity<?> supprimerAnnonce(@PathVariable Long id, @PathVariable Long annonceId) {
        // Sécurité : Seul l'organisateur peut supprimer les annonces de son événement
        if (verifierSiOrganisateur(id)) {
            evenementService.supprimerAnnonce(annonceId);
            return ResponseEntity.ok().build(); // On renvoie un succès 200 sans corps
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Action non autorisée");
    }


    // =======================================================
    // MÉTHODES PRIVÉES (Aide à la sécurité)
    // =======================================================

    private boolean verifierSiOrganisateur(Long evenementId) {
        // 1. On récupère l'événement concerné
        Evenement evenement = evenementService.trouverParId(evenementId);

        // 2. On récupère l'email de l'utilisateur actuellement connecté (Spring Security)
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String email;
        if (principal instanceof UserDetails) {
            email = ((UserDetails) principal).getUsername();
        } else {
            email = principal.toString();
        }

        // 3. On récupère l'objet Utilisateur correspondant
        Utilisateur currentUser = utilisateurService.trouverParEmail(email);

        // 4. On compare les ID : si c'est le même, c'est bien l'organisateur !
        return evenement.getOrganisateur().getId().equals(currentUser.getId());
    }
}