package com.jocf.sporttrack.dto;

import com.jocf.sporttrack.model.Utilisateur;

public class LigneClassementChallenge {

    private final Utilisateur utilisateur;
    private final long score;

    public LigneClassementChallenge(Utilisateur utilisateur, long score) {
        this.utilisateur = utilisateur;
        this.score = score;
    }

    public Utilisateur getUtilisateur() {
        return utilisateur;
    }

    public long getScore() {
        return score;
    }
}