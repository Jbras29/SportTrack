package com.jocf.sporttrack.dto;

import com.jocf.sporttrack.model.TypeSport;

import java.time.LocalDate;
import java.util.List;

public record ModifierActiviteRequest(
        String nom,
        TypeSport typeSport,
        Double distance,
        Integer temps,
        LocalDate date,
        String location,
        Integer evaluation,
        List<Long> inviteIds
) {
    public ModifierActiviteRequest {
        inviteIds = inviteIds == null ? List.of() : inviteIds;
    }
}
