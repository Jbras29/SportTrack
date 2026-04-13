package com.jocf.sporttrack.dto;

import java.time.LocalDate;

public record CreerChallengeRequest(String nom, LocalDate dateDebut, LocalDate dateFin) {
}
