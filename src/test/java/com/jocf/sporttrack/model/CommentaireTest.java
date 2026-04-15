package com.jocf.sporttrack.model;

import org.junit.jupiter.api.Test;

import com.jocf.sporttrack.enumeration.TypeCommentaire;
import com.jocf.sporttrack.enumeration.TypeUtilisateur;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CommentaireTest {

    @Test
    void builder_conserveLesChamps() {
        LocalDateTime date = LocalDateTime.of(2025, 3, 15, 10, 30);
        Utilisateur auteur = Utilisateur.builder()
                .id(1L)
                .nom("D")
                .prenom("J")
                .email("j@test.com")
                .motdepasse("x")
                .typeUtilisateur(TypeUtilisateur.UTILISATEUR)
                .build();
        Activite activite = Activite.builder()
                .id(10L)
                .nom("Run")
                .build();

        Commentaire c = Commentaire.builder()
                .id(100L)
                .type(TypeCommentaire.MESSAGE)
                .message("Bravo !")
                .dateCreation(date)
                .auteur(auteur)
                .activite(activite)
                .build();

        assertThat(c.getId()).isEqualTo(100L);
        assertThat(c.getType()).isEqualTo(TypeCommentaire.MESSAGE);
        assertThat(c.getMessage()).isEqualTo("Bravo !");
        assertThat(c.getDateCreation()).isEqualTo(date);
        assertThat(c.getAuteur()).isSameAs(auteur);
        assertThat(c.getActivite()).isSameAs(activite);
    }
}
