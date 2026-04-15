package com.jocf.sporttrack.controller;

import com.jocf.sporttrack.dto.CommentaireFeedbackResponse;
import com.jocf.sporttrack.dto.CreerCommentaireTexteRequest;
import com.jocf.sporttrack.dto.CreerReactionRequest;
import com.jocf.sporttrack.enumeration.TypeCommentaire;
import com.jocf.sporttrack.model.Commentaire;
import com.jocf.sporttrack.service.CommentaireService;
import com.jocf.sporttrack.service.NonAutoriseException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Commentaires", description = "Gestion des commentaires et réactions sur les activités")
public class CommentaireController {

    private final CommentaireService commentaireService;

    public CommentaireController(CommentaireService commentaireService) {
        this.commentaireService = commentaireService;
    }

    @GetMapping("/commentaires")
    @Operation(summary = "Récupérer tous les commentaires")
    public ResponseEntity<List<Commentaire>> getAllCommentaires() {
        List<Commentaire> commentaires = commentaireService.recupererTousLesCommentaires();
        return ResponseEntity.ok(commentaires);
    }

    @GetMapping("/commentaires/{id}")
    @Operation(summary = "Récupérer un commentaire par ID")
    public ResponseEntity<Commentaire> getCommentaireById(@PathVariable Long id) {
        return commentaireService.trouverParId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping({
            "/commentaires/activite/{activiteId}",
            "/activites/{activiteId}/commentaires"
    })
    @Operation(summary = "Récupérer les commentaires et réactions d'une activité (type MESSAGE ou REACTION)")
    public ResponseEntity<List<Commentaire>> getCommentairesByActivite(@PathVariable Long activiteId) {
        try {
            List<Commentaire> commentaires = commentaireService.recupererCommentairesParActivite(activiteId);
            return ResponseEntity.ok(commentaires);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/commentaires")
    @Operation(summary = "Créer un nouveau commentaire (paramètres query — historique)")
    public ResponseEntity<Commentaire> createCommentaire(@RequestParam Long auteurId, @RequestParam Long activiteId,
                                                         @RequestParam TypeCommentaire type, @RequestParam String message) {
        try {
            Commentaire created = commentaireService.creerCommentaire(auteurId, activiteId, type, message);
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/activites/{activiteId}/commentaires")
    @Operation(summary = "Poster un commentaire textuel sur une activité")
    public ResponseEntity<CommentaireFeedbackResponse> posterCommentaireTexte(
            @PathVariable Long activiteId,
            @RequestBody CreerCommentaireTexteRequest body) {
        if (body == null || body.auteurId() == null) {
            return ResponseEntity.badRequest()
                    .body(CommentaireFeedbackResponse.erreur("Le corps doit contenir auteurId."));
        }
        try {
            Commentaire c = commentaireService.ajouterCommentaireTexte(body.auteurId(), activiteId, body.message());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(CommentaireFeedbackResponse.ok("Commentaire publié.", c));
        } catch (IllegalArgumentException e) {
            return reponseErreurReferenceOuValidation(e);
        }
    }

    @PostMapping("/activites/{activiteId}/reactions")
    @Operation(summary = "Ajouter une réaction emoji sur une activité")
    public ResponseEntity<CommentaireFeedbackResponse> posterReaction(
            @PathVariable Long activiteId,
            @RequestBody CreerReactionRequest body) {
        if (body == null || body.auteurId() == null) {
            return ResponseEntity.badRequest()
                    .body(CommentaireFeedbackResponse.erreur("Le corps doit contenir auteurId."));
        }
        try {
            var resultat = commentaireService.ajouterReactionEmoji(body.auteurId(), activiteId, body.emoji());
            Commentaire c = resultat.commentaire();
            String msg = resultat.nouvellementCree() ? "Réaction enregistrée." : "Réaction déjà présente.";
            HttpStatus status = resultat.nouvellementCree() ? HttpStatus.CREATED : HttpStatus.OK;
            return ResponseEntity.status(status)
                    .body(CommentaireFeedbackResponse.ok(msg, c));
        } catch (IllegalArgumentException e) {
            return reponseErreurReferenceOuValidation(e);
        }
    }

    @PutMapping("/commentaires/{id}")
    @Operation(summary = "Modifier un commentaire")
    public ResponseEntity<Commentaire> updateCommentaire(@PathVariable Long id, @RequestParam String nouveauMessage) {
        try {
            Commentaire updated = commentaireService.modifierCommentaire(id, nouveauMessage);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/commentaires/{id}")
    @Operation(summary = "Supprimer un commentaire (sans vérification d'auteur — historique)")
    public ResponseEntity<Void> deleteCommentaire(@PathVariable Long id) {
        try {
            commentaireService.supprimerCommentaire(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/activites/{activiteId}/commentaires/{commentaireId}")
    @Operation(summary = "Supprimer un commentaire textuel (auteur requis)")
    public ResponseEntity<CommentaireFeedbackResponse> supprimerCommentaireTexte(
            @PathVariable Long activiteId,
            @PathVariable Long commentaireId,
            @RequestParam Long utilisateurId) {
        try {
            commentaireService.supprimerCommentaireTexte(activiteId, commentaireId, utilisateurId);
            return ResponseEntity.ok(CommentaireFeedbackResponse.okSansCommentaire(
                    "Commentaire supprimé.", commentaireId));
        } catch (NonAutoriseException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(CommentaireFeedbackResponse.erreur(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return reponseErreurSelonMessage(e);
        }
    }

    @DeleteMapping("/activites/{activiteId}/reactions/{commentaireId}")
    @Operation(summary = "Supprimer une réaction (auteur requis)")
    public ResponseEntity<CommentaireFeedbackResponse> supprimerReaction(
            @PathVariable Long activiteId,
            @PathVariable Long commentaireId,
            @RequestParam Long utilisateurId) {
        try {
            commentaireService.supprimerReaction(activiteId, commentaireId, utilisateurId);
            return ResponseEntity.ok(CommentaireFeedbackResponse.okSansCommentaire(
                    "Réaction supprimée.", commentaireId));
        } catch (NonAutoriseException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(CommentaireFeedbackResponse.erreur(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return reponseErreurSelonMessage(e);
        }
    }

    private static ResponseEntity<CommentaireFeedbackResponse> reponseErreurSelonMessage(IllegalArgumentException e) {
        String msg = e.getMessage() != null ? e.getMessage() : "";
        if (msg.startsWith("Commentaire introuvable")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CommentaireFeedbackResponse.erreur(msg));
        }
        if (msg.contains("n'appartient pas à cette activité")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CommentaireFeedbackResponse.erreur(msg));
        }
        return ResponseEntity.badRequest()
                .body(CommentaireFeedbackResponse.erreur(msg));
    }

    /** Activité ou auteur inconnu → 404 ; message invalide → 400. */
    private static ResponseEntity<CommentaireFeedbackResponse> reponseErreurReferenceOuValidation(
            IllegalArgumentException e) {
        String msg = e.getMessage() != null ? e.getMessage() : "";
        if (msg.startsWith("Activite introuvable") || msg.startsWith("Auteur introuvable")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CommentaireFeedbackResponse.erreur(msg));
        }
        return ResponseEntity.badRequest()
                .body(CommentaireFeedbackResponse.erreur(msg));
    }
}
