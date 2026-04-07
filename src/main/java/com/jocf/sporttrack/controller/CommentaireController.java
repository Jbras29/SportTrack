package com.jocf.sporttrack.controller;

import com.jocf.sporttrack.model.Commentaire;
import com.jocf.sporttrack.model.TypeCommentaire;
import com.jocf.sporttrack.service.CommentaireService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/commentaires")
@Tag(name = "Commentaires", description = "Gestion des commentaires")
public class CommentaireController {

    private final CommentaireService commentaireService;

    public CommentaireController(CommentaireService commentaireService) {
        this.commentaireService = commentaireService;
    }

    @GetMapping
    @Operation(summary = "Récupérer tous les commentaires")
    public ResponseEntity<List<Commentaire>> getAllCommentaires() {
        List<Commentaire> commentaires = commentaireService.recupererTousLesCommentaires();
        return ResponseEntity.ok(commentaires);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Récupérer un commentaire par ID")
    public ResponseEntity<Commentaire> getCommentaireById(@PathVariable Long id) {
        return commentaireService.trouverParId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/activite/{activiteId}")
    @Operation(summary = "Récupérer les commentaires d'une activité")
    public ResponseEntity<List<Commentaire>> getCommentairesByActivite(@PathVariable Long activiteId) {
        try {
            List<Commentaire> commentaires = commentaireService.recupererCommentairesParActivite(activiteId);
            return ResponseEntity.ok(commentaires);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @Operation(summary = "Créer un nouveau commentaire")
    public ResponseEntity<Commentaire> createCommentaire(@RequestParam Long auteurId, @RequestParam Long activiteId,
                                                         @RequestParam TypeCommentaire type, @RequestParam String message) {
        try {
            Commentaire created = commentaireService.creerCommentaire(auteurId, activiteId, type, message);
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un commentaire")
    public ResponseEntity<Commentaire> updateCommentaire(@PathVariable Long id, @RequestParam String nouveauMessage) {
        try {
            Commentaire updated = commentaireService.modifierCommentaire(id, nouveauMessage);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un commentaire")
    public ResponseEntity<Void> deleteCommentaire(@PathVariable Long id) {
        try {
            commentaireService.supprimerCommentaire(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}