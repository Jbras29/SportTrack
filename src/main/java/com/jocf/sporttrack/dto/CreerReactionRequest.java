package com.jocf.sporttrack.dto;

/**
 * Corps JSON pour ajouter une réaction emoji (stockée comme {@code Commentaire} de type REACTION).
 */
public record CreerReactionRequest(Long auteurId, String emoji) {
}
