package com.jocf.sporttrack.dto;

/**
 * Corps JSON pour poster un commentaire textuel sur une activité.
 */
public record CreerCommentaireTexteRequest(Long auteurId, String message) {
}
