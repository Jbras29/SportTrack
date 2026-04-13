package com.jocf.sporttrack.controller;

import com.jocf.sporttrack.dto.CreerEvenementRequest;
import com.jocf.sporttrack.dto.ModifierEvenementRequest;
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
import java.util.Map;
import java.util.stream.Collectors;

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
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String email = (principal instanceof UserDetails) ? ((UserDetails) principal).getUsername() : principal.toString();
        Utilisateur currentUser = utilisateurService.trouverParEmail(email);
        model.addAttribute("user", currentUser);

        List<Evenement> listeEvenements = evenementService.obtenirTousLesEvenements();
        model.addAttribute("evenements", listeEvenements);

        return "evenement/evenement";
    }

    @GetMapping("/creer-evenement")
    public String afficherPageCreerEvenement(Model model) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String email = (principal instanceof UserDetails) ? ((UserDetails) principal).getUsername() : principal.toString();
        Utilisateur currentUser = utilisateurService.trouverParEmail(email);

        model.addAttribute("user", currentUser);
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

        boolean isParticipant = evenement.getParticipants().stream()
                .anyMatch(p -> p.getId().equals(currentUser.getId()));

        boolean afficherNomOrganisateur = utilisateurService.peutAfficherIdentiteVers(
                evenement.getOrganisateur(), currentUser.getId());
        Map<Long, Boolean> afficherNomParticipant = evenement.getParticipants().stream()
                .collect(Collectors.toMap(
                        Utilisateur::getId,
                        p -> utilisateurService.peutAfficherIdentiteVers(p, currentUser.getId()),
                        (a, b) -> a));

        model.addAttribute("evenement", evenement);
        model.addAttribute("isOrganisateur", isOrganisateur);
        model.addAttribute("isParticipant", isParticipant);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("afficherNomOrganisateur", afficherNomOrganisateur);
        model.addAttribute("afficherNomParticipant", afficherNomParticipant);

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
    public ResponseEntity<Evenement> creerEvenement(@RequestBody CreerEvenementRequest body) {
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
        Evenement evenementCree = evenementService.creerEvenement(organisateur.getId(), body);

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
    public ResponseEntity<?> modifierEvenement(@PathVariable Long id, @RequestBody ModifierEvenementRequest nouveauxDetails) {
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


    // API pour retirer (kicker) un participant
    @ResponseBody
    @DeleteMapping("/api/evenements/{id}/participants/{userId}")
    public ResponseEntity<?> kickParticipant(@PathVariable Long id, @PathVariable Long userId) {
        // Sécurité : Seul l'organisateur peut exclure quelqu'un
        if (verifierSiOrganisateur(id)) {
            evenementService.retirerParticipant(id, userId);
            return ResponseEntity.ok().build(); // Succès 200
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Action non autorisée.");
    }

    @ResponseBody
    @PostMapping("/api/evenements/{id}/quitter")
    public ResponseEntity<?> quitter(@PathVariable Long id) {

        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String email = (principal instanceof UserDetails) ? ((UserDetails) principal).getUsername() : principal.toString();
        Utilisateur currentUser = utilisateurService.trouverParEmail(email);

        evenementService.quitterEvenement(id, currentUser.getId());
        return ResponseEntity.ok().build();
    }

    @ResponseBody
    @DeleteMapping("/api/evenements/{id}")
    public ResponseEntity<?> supprimerEvenement(@PathVariable Long id) {

        Evenement evenement = evenementService.trouverParId(id);


        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String email = (principal instanceof UserDetails) ? ((UserDetails) principal).getUsername() : principal.toString();
        Utilisateur currentUser = utilisateurService.trouverParEmail(email);

        if (!evenement.getOrganisateur().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Seul l'organisateur peut supprimer cet événement.");
        }

        evenementService.supprimer(id);
        return ResponseEntity.ok().build();
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