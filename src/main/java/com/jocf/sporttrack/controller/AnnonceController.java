package com.jocf.sporttrack.controller;

import com.jocf.sporttrack.model.Annonce;
import com.jocf.sporttrack.service.AnnonceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/annonces")
@Tag(name = "Annonces", description = "Gestion des annonces")
public class AnnonceController {

    private final AnnonceService annonceService;

    public AnnonceController(AnnonceService annonceService) {
        this.annonceService = annonceService;
    }

    @GetMapping
    @Operation(summary = "Récupérer toutes les annonces")
    public ResponseEntity<List<Annonce>> getAllAnnonces() {
        List<Annonce> annonces = annonceService.recupererToutesLesAnnonces();
        return ResponseEntity.ok(annonces);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Récupérer une annonce par ID")
    public ResponseEntity<Annonce> getAnnonceById(@PathVariable Long id) {
        return annonceService.trouverParId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/evenement/{evenementId}")
    @Operation(summary = "Récupérer les annonces d'un événement")
    public ResponseEntity<List<Annonce>> getAnnoncesByEvenement(@PathVariable Long evenementId) {
        try {
            List<Annonce> annonces = annonceService.recupererAnnoncesParEvenement(evenementId);
            return ResponseEntity.ok(annonces);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/dashboard/{utilisateurId}")
    @Operation(summary = "Récupérer les annonces du dashboard d'un utilisateur")
    public ResponseEntity<List<Annonce>> getAnnoncesDashboard(@PathVariable Long utilisateurId) {
        try {
            List<Annonce> annonces = annonceService.recupererAnnoncesParParticipant(utilisateurId);
            return ResponseEntity.ok(annonces);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @Operation(summary = "Créer une nouvelle annonce")
    public ResponseEntity<Annonce> createAnnonce(@RequestParam Long evenementId, @RequestParam String message) {
        try {
            Annonce created = annonceService.creerAnnonce(evenementId, message);
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier une annonce")
    public ResponseEntity<Annonce> updateAnnonce(@PathVariable Long id, @RequestParam String nouveauMessage) {
        try {
            Annonce updated = annonceService.modifierAnnonce(id, nouveauMessage);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une annonce")
    public ResponseEntity<Void> deleteAnnonce(@PathVariable Long id) {
        try {
            annonceService.supprimerAnnonce(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}