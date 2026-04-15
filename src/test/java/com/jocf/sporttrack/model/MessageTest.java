package com.jocf.sporttrack.model;

import org.junit.jupiter.api.Test;

import com.jocf.sporttrack.enumeration.TypeUtilisateur;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires de la logique métier sur {@link Message} (hors persistance).
 */
class MessageTest {

    private static final Long USER1_ID = 1L;
    private static final Long USER2_ID = 2L;

    private Utilisateur utilisateur(Long id) {
        return Utilisateur.builder()
                .id(id)
                .nom("N" + id)
                .prenom("P" + id)
                .email("u" + id + "@test.com")
                .motdepasse("x")
                .typeUtilisateur(TypeUtilisateur.UTILISATEUR)
                .build();
    }

    @Test
    void marquerCommeLu_premierAppel_metLuEtDateLu() {
        Message m = Message.builder()
                .id(10L)
                .expediteur(utilisateur(USER1_ID))
                .destinataire(utilisateur(USER2_ID))
                .contenu("hello")
                .dateEnvoi(LocalDateTime.now().minusMinutes(1))
                .lu(false)
                .dateLu(null)
                .build();

        m.marquerCommeLu();

        assertThat(m.isLu()).isTrue();
        assertThat(m.getDateLu()).isNotNull();
    }

    @Test
    void marquerCommeLu_deuxiemeAppel_idempotent_conserveDateLu() {
        Message m = Message.builder()
                .id(10L)
                .expediteur(utilisateur(USER1_ID))
                .destinataire(utilisateur(USER2_ID))
                .contenu("hello")
                .dateEnvoi(LocalDateTime.now().minusMinutes(1))
                .lu(false)
                .dateLu(null)
                .build();

        m.marquerCommeLu();
        LocalDateTime premiere = m.getDateLu();

        m.marquerCommeLu();

        assertThat(m.isLu()).isTrue();
        assertThat(m.getDateLu()).isEqualTo(premiere);
    }

    @Test
    void marquerCommeLu_dejaLu_neModifiePasDateLu() {
        LocalDateTime deja = LocalDateTime.of(2025, 1, 2, 12, 0);
        Message m = Message.builder()
                .id(10L)
                .expediteur(utilisateur(USER1_ID))
                .destinataire(utilisateur(USER2_ID))
                .contenu("hello")
                .dateEnvoi(LocalDateTime.now().minusMinutes(1))
                .lu(true)
                .dateLu(deja)
                .build();

        m.marquerCommeLu();

        assertThat(m.isLu()).isTrue();
        assertThat(m.getDateLu()).isEqualTo(deja);
    }
}
