package com.jocf.sporttrack.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Données de création d'événement (corps JSON), sans entité persistante.
 */
public record CreerEvenementRequest(
        String nom,
        String description,
        LocalDateTime date,
        List<ParticipantIdRef> participants
) {
    public CreerEvenementRequest {
        participants = participants == null ? List.of() : participants;
    }

    public record ParticipantIdRef(Long id) {
    }
}
