package com.jocf.sporttrack.web;

import com.jocf.sporttrack.model.TypeUtilisateur;
import com.jocf.sporttrack.model.Utilisateur;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SessionUtilisateurTest {

    @Test
    void from_mappeLesChampsPrincipaux() {
        Utilisateur user = Utilisateur.builder()
                .id(3L)
                .email("jane@test.com")
                .nom("Doe")
                .prenom("Jane")
                .motdepasse("x")
                .typeUtilisateur(TypeUtilisateur.UTILISATEUR)
                .build();

        SessionUtilisateur sessionUtilisateur = SessionUtilisateur.from(user);

        assertThat(sessionUtilisateur.id()).isEqualTo(3L);
        assertThat(sessionUtilisateur.email()).isEqualTo("jane@test.com");
        assertThat(sessionUtilisateur.nom()).isEqualTo("Doe");
        assertThat(sessionUtilisateur.prenom()).isEqualTo("Jane");
    }
}
