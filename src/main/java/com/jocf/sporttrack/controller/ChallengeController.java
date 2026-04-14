package com.jocf.sporttrack.controller;

import com.jocf.sporttrack.dto.CreerChallengeRequest;
import com.jocf.sporttrack.dto.ModifierChallengeRequest;
import com.jocf.sporttrack.model.Challenge;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.service.ChallengeService;
import com.jocf.sporttrack.service.UtilisateurService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/api/challenges")
@Tag(name = "Challenges", description = "Gestion des challenges")
public class ChallengeController {

    private final ChallengeService challengeService;
    private final UtilisateurService utilisateurService;

    public ChallengeController(ChallengeService challengeService, UtilisateurService utilisateurService) {
        this.challengeService = challengeService;
        this.utilisateurService = utilisateurService;
    }

    @GetMapping
    @Operation(summary = "Récupérer tous les challenges")
    public ResponseEntity<List<Challenge>> getAllChallenges() {
        List<Challenge> challenges = challengeService.recupererTousLesChallenges();
        return ResponseEntity.ok(challenges);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Récupérer un challenge par ID")
    public ResponseEntity<Challenge> getChallengeById(@PathVariable Long id) {
        return challengeService.trouverParId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Créer un nouveau challenge")
    public ResponseEntity<Object> createChallenge(@RequestBody CreerChallengeRequest body, @RequestParam Long organisateurId) {

        Optional<Utilisateur> organisateurOpt = utilisateurService.trouverParId(organisateurId);


        if (organisateurOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Utilisateur non trouvé.");
        }


        Utilisateur organisateur = organisateurOpt.get();

        if (organisateur.getHpNormalise() <= 0) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Action impossible : Votre barre de vie est à 0.");
        }

        try {

            Challenge created = challengeService.creerChallenge(body, organisateurId);
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un challenge")
    public ResponseEntity<Challenge> updateChallenge(@PathVariable Long id, @RequestBody ModifierChallengeRequest body) {
        try {
            Challenge updated = challengeService.modifierChallenge(id, body);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un challenge")
    public ResponseEntity<Void> deleteChallenge(@PathVariable Long id) {
        try {
            challengeService.supprimerChallenge(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    

    
}