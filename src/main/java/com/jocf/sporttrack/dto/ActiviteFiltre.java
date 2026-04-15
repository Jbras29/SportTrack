package com.jocf.sporttrack.dto;

import com.jocf.sporttrack.enumeration.TypeSport;
import com.jocf.sporttrack.model.Utilisateur;

import java.time.LocalDate;

/**
 * Critères optionnels pour le filtrage avancé d’activités (repository).
 */
public record ActiviteFiltre(
        Utilisateur utilisateur,
        TypeSport typeSport,
        Double distanceMin,
        Double distanceMax,
        Integer tempsMin,
        Integer tempsMax,
        LocalDate dateDebut,
        LocalDate dateFin,
        String location) {
}
