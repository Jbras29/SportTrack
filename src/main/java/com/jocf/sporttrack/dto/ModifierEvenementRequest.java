package com.jocf.sporttrack.dto;

import java.time.LocalDateTime;

public record ModifierEvenementRequest(String nom, String description, LocalDateTime date) {
}
