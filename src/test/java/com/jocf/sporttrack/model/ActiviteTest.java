package com.jocf.sporttrack.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ActiviteTest {

    // ── Builder & champs de base ─────────────────────────────────────────────

    @Test
    void builderCreeUneActiviteAvecLesChampsDBase() {
        LocalDate date = LocalDate.of(2025, 6, 15);
        Utilisateur user = Utilisateur.builder().id(1L).build();

        Activite activite = Activite.builder()
                .id(1L)
                .nom("Course du dimanche")
                .typeSport(TypeSport.COURSE_A_PIED)
                .date(date)
                .distance(10.0)
                .temps(60)
                .location("Paris")
                .evaluation(5)
                .xpGagne(42)
                .calories(700.0)
                .meteoTemperature(18.5)
                .meteoCondition("Ensoleillé")
                .utilisateur(user)
                .build();

        assertThat(activite.getId()).isEqualTo(1L);
        assertThat(activite.getNom()).isEqualTo("Course du dimanche");
        assertThat(activite.getTypeSport()).isEqualTo(TypeSport.COURSE_A_PIED);
        assertThat(activite.getDate()).isEqualTo(date);
        assertThat(activite.getDistance()).isEqualTo(10.0);
        assertThat(activite.getTemps()).isEqualTo(60);
        assertThat(activite.getLocation()).isEqualTo("Paris");
        assertThat(activite.getEvaluation()).isEqualTo(5);
        assertThat(activite.getXpGagne()).isEqualTo(42);
        assertThat(activite.getCalories()).isEqualTo(700.0);
        assertThat(activite.getMeteoTemperature()).isEqualTo(18.5);
        assertThat(activite.getMeteoCondition()).isEqualTo("Ensoleillé");
        assertThat(activite.getUtilisateur()).isSameAs(user);
    }

    @Test
    void constructeurParDefautInitialiseListesVides() {
        Activite activite = new Activite();

        assertThat(activite.getCommentaires()).isNotNull().isEmpty();
        assertThat(activite.getInvites()).isNotNull().isEmpty();
        assertThat(activite.getCalories()).isNull();
        assertThat(activite.getMeteoCondition()).isNull();
        assertThat(activite.getMeteoTemperature()).isNull();
    }

    @Test
    void getLimiteReactionsAffichees_retourne5() {
        assertThat(Activite.getLimiteReactionsAffichees()).isEqualTo(5);
    }

    // ── Calories & Météo ─────────────────────────────────────────────────────

    @Test
    void caloriesEtMeteoSontNull_parDefaut() {
        Activite a = Activite.builder()
                .nom("Yoga")
                .typeSport(TypeSport.YOGA)
                .date(LocalDate.now())
                .utilisateur(Utilisateur.builder().id(1L).build())
                .build();

        assertThat(a.getCalories()).isNull();
        assertThat(a.getMeteoTemperature()).isNull();
        assertThat(a.getMeteoCondition()).isNull();
    }

    @Test
    void setCaloriesEtMeteoMiseAJourCorrecte() {
        Activite a = Activite.builder()
                .nom("Natation")
                .typeSport(TypeSport.NATATION)
                .date(LocalDate.now())
                .utilisateur(Utilisateur.builder().id(2L).build())
                .build();

        a.setCalories(350.0);
        a.setMeteoTemperature(22.5);
        a.setMeteoCondition("Nuageux");

        assertThat(a.getCalories()).isEqualTo(350.0);
        assertThat(a.getMeteoTemperature()).isEqualTo(22.5);
        assertThat(a.getMeteoCondition()).isEqualTo("Nuageux");
    }

    // ── Commentaires & Réactions ─────────────────────────────────────────────

    @Test
    void getCommentairesMessages_retourneSeulementLesMessages() {
        Utilisateur auteur = Utilisateur.builder().id(1L).prenom("Alice").nom("D").build();

        Commentaire msg = Commentaire.builder()
                .id(1L).type(TypeCommentaire.MESSAGE)
                .message("Bravo !").auteur(auteur).build();
        Commentaire reaction = Commentaire.builder()
                .id(2L).type(TypeCommentaire.REACTION)
                .message("👍").auteur(auteur).build();

        Activite activite = Activite.builder()
                .nom("test")
                .typeSport(TypeSport.COURSE)
                .date(LocalDate.now())
                .utilisateur(auteur)
                .build();
        activite.getCommentaires().add(msg);
        activite.getCommentaires().add(reaction);

        List<Commentaire> messages = activite.getCommentairesMessages();

        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getMessage()).isEqualTo("Bravo !");
    }

    @Test
    void getCommentairesMessages_listeVideSiAucunComentaire() {
        Activite activite = Activite.builder()
                .nom("test")
                .typeSport(TypeSport.YOGA)
                .date(LocalDate.now())
                .utilisateur(Utilisateur.builder().id(1L).build())
                .build();

        assertThat(activite.getCommentairesMessages()).isEmpty();
    }

    @Test
    void getReactionsGroupees_agrageLesReactionsMemeEmoji() {
        Utilisateur u1 = Utilisateur.builder().id(1L).prenom("Alice").nom("A").build();
        Utilisateur u2 = Utilisateur.builder().id(2L).prenom("Bob").nom("B").build();

        Commentaire r1 = Commentaire.builder()
                .id(1L).type(TypeCommentaire.REACTION).message("👍").auteur(u1).build();
        Commentaire r2 = Commentaire.builder()
                .id(2L).type(TypeCommentaire.REACTION).message("👍").auteur(u2).build();
        Commentaire r3 = Commentaire.builder()
                .id(3L).type(TypeCommentaire.REACTION).message("🔥").auteur(u1).build();

        Activite activite = Activite.builder()
                .nom("test")
                .typeSport(TypeSport.FOOTBALL)
                .date(LocalDate.now())
                .utilisateur(u1)
                .build();
        activite.getCommentaires().addAll(List.of(r1, r2, r3));

        List<ReactionGroupee> groupees = activite.getReactionsGroupees();

        assertThat(groupees).hasSize(2);
        ReactionGroupee thumbs = groupees.stream()
                .filter(g -> g.emoji().equals("👍")).findFirst().orElseThrow();
        assertThat(thumbs.nombre()).isEqualTo(2);
        assertThat(thumbs.nomsDesReacteurs()).contains("Alice A", "Bob B");
    }

    @Test
    void getReactionsGroupeesAffichees_limiteeA5() {
        Utilisateur u = Utilisateur.builder().id(1L).prenom("X").nom("Y").build();
        Activite activite = Activite.builder()
                .nom("test")
                .typeSport(TypeSport.BOXE)
                .date(LocalDate.now())
                .utilisateur(u)
                .build();

        // 6 réactions avec 6 emojis différents
        String[] emojis = {"😀", "😂", "🔥", "👍", "❤️", "🎉"};
        int i = 1;
        for (String emoji : emojis) {
            Utilisateur reacteur = Utilisateur.builder().id((long) i).prenom("U" + i).nom("N").build();
            activite.getCommentaires().add(
                    Commentaire.builder().id((long) i).type(TypeCommentaire.REACTION)
                            .message(emoji).auteur(reacteur).build());
            i++;
        }

        assertThat(activite.getReactionsGroupees()).hasSize(6);
        assertThat(activite.getReactionsGroupeesAffichees()).hasSize(5);
        assertThat(activite.getReactionsGroupeesMasqueesCount()).isEqualTo(1);
    }

    @Test
    void getReactionsGroupeesMasqueesCount_retourne0SiMoinsQue5() {
        Utilisateur u = Utilisateur.builder().id(1L).prenom("A").nom("B").build();
        Activite activite = Activite.builder()
                .nom("test")
                .typeSport(TypeSport.TENNIS)
                .date(LocalDate.now())
                .utilisateur(u)
                .build();
        activite.getCommentaires().add(
                Commentaire.builder().id(1L).type(TypeCommentaire.REACTION)
                        .message("👍").auteur(u).build());

        assertThat(activite.getReactionsGroupeesMasqueesCount()).isEqualTo(0);
    }

    @Test
    void utilisateurAEmitReactionAvecEmoji_trouveReactionExistante() {
        Utilisateur u = Utilisateur.builder().id(1L).prenom("A").nom("B").build();
        Commentaire reaction = Commentaire.builder()
                .id(10L).type(TypeCommentaire.REACTION).message("🔥").auteur(u).build();

        Activite activite = Activite.builder()
                .nom("test").typeSport(TypeSport.CYCLISME)
                .date(LocalDate.now()).utilisateur(u).build();
        activite.getCommentaires().add(reaction);

        assertThat(activite.utilisateurAEmitReactionAvecEmoji(1L, "🔥")).isTrue();
        assertThat(activite.utilisateurAEmitReactionAvecEmoji(1L, "💀")).isFalse();
        assertThat(activite.utilisateurAEmitReactionAvecEmoji(99L, "🔥")).isFalse();
    }

    @Test
    void utilisateurAEmitReactionAvecEmoji_avecIdOuEmojiNull_retourneFalse() {
        Activite activite = Activite.builder()
                .nom("t").typeSport(TypeSport.YOGA)
                .date(LocalDate.now())
                .utilisateur(Utilisateur.builder().id(1L).build())
                .build();

        assertThat(activite.utilisateurAEmitReactionAvecEmoji(null, "👍")).isFalse();
        assertThat(activite.utilisateurAEmitReactionAvecEmoji(1L, null)).isFalse();
    }

    @Test
    void getIdCommentaireReactionUtilisateur_retourneLId() {
        Utilisateur u = Utilisateur.builder().id(5L).prenom("A").nom("B").build();
        Commentaire reaction = Commentaire.builder()
                .id(42L).type(TypeCommentaire.REACTION).message("❤️").auteur(u).build();

        Activite activite = Activite.builder()
                .nom("t").typeSport(TypeSport.NATATION)
                .date(LocalDate.now()).utilisateur(u).build();
        activite.getCommentaires().add(reaction);

        assertThat(activite.getIdCommentaireReactionUtilisateur(5L, "❤️")).isEqualTo(42L);
        assertThat(activite.getIdCommentaireReactionUtilisateur(5L, "👍")).isNull();
    }

    @Test
    void getReactionsParEmoji_retourneMapAvecCounts() {
        Utilisateur u1 = Utilisateur.builder().id(1L).prenom("A").nom("B").build();
        Utilisateur u2 = Utilisateur.builder().id(2L).prenom("C").nom("D").build();

        Activite activite = Activite.builder()
                .nom("t").typeSport(TypeSport.FOOTBALL)
                .date(LocalDate.now()).utilisateur(u1).build();
        activite.getCommentaires().add(
                Commentaire.builder().id(1L).type(TypeCommentaire.REACTION).message("👍").auteur(u1).build());
        activite.getCommentaires().add(
                Commentaire.builder().id(2L).type(TypeCommentaire.REACTION).message("👍").auteur(u2).build());
        activite.getCommentaires().add(
                Commentaire.builder().id(3L).type(TypeCommentaire.REACTION).message("🔥").auteur(u1).build());

        var map = activite.getReactionsParEmoji();

        assertThat(map).containsEntry("👍", 2L).containsEntry("🔥", 1L);
    }
}
