package com.jocf.sporttrack.dto;

import com.jocf.sporttrack.model.Commentaire;

/**
 * Réponse JSON unifiée pour les opérations sur commentaires / réactions (succès ou échec explicite).
 */
public record CommentaireFeedbackResponse(
        boolean success,
        String message,
        Long commentaireId,
        Commentaire commentaire
) {
    public static CommentaireFeedbackResponse ok(String message, Commentaire commentaire) {
        Long id = commentaire != null ? commentaire.getId() : null;
        return new CommentaireFeedbackResponse(true, message, id, commentaire);
    }

    public static CommentaireFeedbackResponse okSansCommentaire(String message, Long commentaireId) {
        return new CommentaireFeedbackResponse(true, message, commentaireId, null);
    }

    public static CommentaireFeedbackResponse erreur(String message) {
        return new CommentaireFeedbackResponse(false, message, null, null);
    }
}
