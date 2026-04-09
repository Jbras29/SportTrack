package com.jocf.sporttrack.service;

import com.jocf.sporttrack.model.Commentaire;

/**
 * Résultat de {@link CommentaireService#ajouterReactionEmoji} :
 * nouvelle ligne ou réaction déjà enregistrée (même auteur, même emoji, même activité).
 */
public record ReactionAjoutResultat(Commentaire commentaire, boolean nouvellementCree) {
}
