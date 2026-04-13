package com.jocf.sporttrack.dto;

import java.time.LocalDate;

public record ModifierChallengeRequest(String nom, LocalDate dateDebut, LocalDate dateFin) {
}
