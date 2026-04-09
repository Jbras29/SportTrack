package com.jocf.sporttrack.model;

/**
 * Réactions agrégées par emoji pour l’affichage (fil d’actualité, tooltips).
 */
public record ReactionGroupee(String emoji, long nombre, String nomsDesReacteurs) {}
