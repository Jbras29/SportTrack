package com.jocf.sporttrack.controller;

import com.jocf.sporttrack.model.Challenge;
import com.jocf.sporttrack.service.ChallengeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/challenges")
@Tag(name = "Challenges", description = "Gestion des challenges")
public class ChallengeController {

    private final ChallengeService challengeService;

    public ChallengeController(ChallengeService challengeService) {
        this.challengeService = challengeService;
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
    public ResponseEntity<Challenge> createChallenge(@RequestBody Challenge challenge, @RequestParam Long organisateurId) {
        try {
            Challenge created = challengeService.creerChallenge(challenge, organisateurId);
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un challenge")
    public ResponseEntity<Challenge> updateChallenge(@PathVariable Long id, @RequestBody Challenge challengeDetails) {
        try {
            Challenge updated = challengeService.modifierChallenge(id, challengeDetails);
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