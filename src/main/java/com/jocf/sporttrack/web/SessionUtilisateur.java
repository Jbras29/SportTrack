package com.jocf.sporttrack.web;

import com.jocf.sporttrack.model.Utilisateur;

import java.io.Serializable;

public record SessionUtilisateur(
        Long id,
        String email,
        String nom,
        String prenom
) implements Serializable {

    private static final long serialVersionUID = 1L;

    public static SessionUtilisateur from(Utilisateur u) {
        return new SessionUtilisateur(u.getId(), u.getEmail(), u.getNom(), u.getPrenom());
    }
}
