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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/evenements")
public class EvenementController {

    private final EvenementService evenementService;
    private final UtilisateurService  utilisateurService;

    @Autowired
    public EvenementController(EvenementService evenementService, UtilisateurService utilisateurService) {
        this.evenementService = evenementService;
        this.utilisateurService = utilisateurService;
    }

    // Endpoint pour créer un nouvel événement
    // Exemple de requête POST: /api/evenements/creer?organisateurId=1
    @PostMapping("/creer")
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

    // Endpoint pour récupérer les événements d'un organisateur
    // Exemple de requête GET: /api/evenements/organisateur/1
    @GetMapping("/organisateur/{organisateurId}")
    public ResponseEntity<List<Evenement>> getEvenementsByOrganisateur(@PathVariable Long organisateurId) {
        List<Evenement> evenements = evenementService.obtenirEvenementsParOrganisateur(organisateurId);
        return ResponseEntity.ok(evenements);
    }
}