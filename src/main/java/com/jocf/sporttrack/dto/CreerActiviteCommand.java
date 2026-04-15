package com.jocf.sporttrack.dto;

import com.jocf.sporttrack.enumeration.TypeSport;

import java.time.LocalDate;
import java.util.List;

/**
 * Paramètres de création d’activité (formulaire HTML / service).
 */
public record CreerActiviteCommand(
        Long utilisateurId,
        String nom,
        TypeSport typeSport,
        LocalDate date,
        Double distance,
        Integer temps,
        String location,
        Integer evaluation,
        List<Long> invitesIds) {
}
