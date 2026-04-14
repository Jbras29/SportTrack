package com.jocf.sporttrack.dto;

import com.jocf.sporttrack.model.Commentaire;
import com.jocf.sporttrack.model.NotificationType;
import com.jocf.sporttrack.model.TypeSport;
import com.jocf.sporttrack.model.TypeUtilisateur;
import com.jocf.sporttrack.model.Utilisateur;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Couverture des DTO (records / beans) utilisés par les contrôleurs et la sérialisation JSON.
 */
class DtoCoverageTest {

    @Nested
    class CommentaireFeedbackResponseTest {

        @Test
        void ok_avecCommentaire_exposeIdDuCommentaire() {
            Commentaire c = new Commentaire();
            c.setId(42L);

            CommentaireFeedbackResponse r = CommentaireFeedbackResponse.ok("msg", c);

            assertThat(r.success()).isTrue();
            assertThat(r.message()).isEqualTo("msg");
            assertThat(r.commentaireId()).isEqualTo(42L);
            assertThat(r.commentaire()).isSameAs(c);
        }

        @Test
        void ok_avecCommentaireNull_idNull() {
            CommentaireFeedbackResponse r = CommentaireFeedbackResponse.ok("x", null);

            assertThat(r.success()).isTrue();
            assertThat(r.commentaireId()).isNull();
            assertThat(r.commentaire()).isNull();
        }

        @Test
        void okSansCommentaire() {
            CommentaireFeedbackResponse r = CommentaireFeedbackResponse.okSansCommentaire("done", 99L);

            assertThat(r.success()).isTrue();
            assertThat(r.commentaireId()).isEqualTo(99L);
            assertThat(r.commentaire()).isNull();
        }

        @Test
        void erreur() {
            CommentaireFeedbackResponse r = CommentaireFeedbackResponse.erreur("échec");

            assertThat(r.success()).isFalse();
            assertThat(r.message()).isEqualTo("échec");
            assertThat(r.commentaireId()).isNull();
            assertThat(r.commentaire()).isNull();
        }
    }

    @Nested
    class CreerChallengeRequestTest {

        @Test
        void accessors() {
            LocalDate d0 = LocalDate.of(2026, 1, 1);
            LocalDate d1 = LocalDate.of(2026, 2, 1);
            CreerChallengeRequest r = new CreerChallengeRequest("C", d0, d1);

            assertThat(r.nom()).isEqualTo("C");
            assertThat(r.dateDebut()).isEqualTo(d0);
            assertThat(r.dateFin()).isEqualTo(d1);
        }
    }

    @Nested
    class CreerCommentaireTexteRequestTest {

        @Test
        void accessors() {
            CreerCommentaireTexteRequest r = new CreerCommentaireTexteRequest(3L, "hello");

            assertThat(r.auteurId()).isEqualTo(3L);
            assertThat(r.message()).isEqualTo("hello");
        }
    }

    @Nested
    class CreerCompteRequestTest {

        @Test
        void gettersSetters() {
            CreerCompteRequest req = new CreerCompteRequest();
            req.setNom("N");
            req.setPrenom("P");
            req.setEmail("e@x.fr");
            req.setMotdepasse("secret");
            req.setSexe("M");
            req.setAge(30);
            req.setPoids(70.5);
            req.setTaille(1.75);

            assertThat(req.getNom()).isEqualTo("N");
            assertThat(req.getPrenom()).isEqualTo("P");
            assertThat(req.getEmail()).isEqualTo("e@x.fr");
            assertThat(req.getMotdepasse()).isEqualTo("secret");
            assertThat(req.getSexe()).isEqualTo("M");
            assertThat(req.getAge()).isEqualTo(30);
            assertThat(req.getPoids()).isEqualTo(70.5);
            assertThat(req.getTaille()).isEqualTo(1.75);
        }
    }

    @Nested
    class CreerEvenementRequestTest {

        @Test
        void participantsNull_devientListeVide() {
            CreerEvenementRequest r = new CreerEvenementRequest("E", "d", LocalDateTime.now(), null);

            assertThat(r.participants()).isEmpty();
        }

        @Test
        void participantsConserves() {
            List<CreerEvenementRequest.ParticipantIdRef> parts =
                    List.of(new CreerEvenementRequest.ParticipantIdRef(1L));
            CreerEvenementRequest r = new CreerEvenementRequest("E", "d", LocalDateTime.now(), parts);

            assertThat(r.participants()).containsExactly(new CreerEvenementRequest.ParticipantIdRef(1L));
        }

        @Test
        void participantIdRef_accessor() {
            assertThat(new CreerEvenementRequest.ParticipantIdRef(5L).id()).isEqualTo(5L);
        }
    }

    @Nested
    class CreerReactionRequestTest {

        @Test
        void accessors() {
            CreerReactionRequest r = new CreerReactionRequest(8L, "👍");

            assertThat(r.auteurId()).isEqualTo(8L);
            assertThat(r.emoji()).isEqualTo("👍");
        }
    }

    @Nested
    class LigneClassementChallengeTest {

        @Test
        void getters() {
            Utilisateur u = Utilisateur.builder()
                    .id(1L)
                    .nom("A")
                    .prenom("B")
                    .email("a@b.c")
                    .motdepasse("x")
                    .typeUtilisateur(TypeUtilisateur.UTILISATEUR)
                    .build();
            LigneClassementChallenge ligne = new LigneClassementChallenge(u, 100L);

            assertThat(ligne.getUtilisateur()).isSameAs(u);
            assertThat(ligne.getScore()).isEqualTo(100L);
        }
    }

    @Nested
    class ModifierActiviteRequestTest {

        @Test
        void inviteIdsNull_devientListeVide() {
            ModifierActiviteRequest r = new ModifierActiviteRequest(
                    "Acti",
                    TypeSport.FOOTBALL,
                    10.0,
                    60,
                    LocalDate.now(),
                    "Paris",
                    4,
                    null);

            assertThat(r.inviteIds()).isEmpty();
        }

        @Test
        void inviteIdsConserves() {
            ModifierActiviteRequest r = new ModifierActiviteRequest(
                    "Acti",
                    TypeSport.FOOTBALL,
                    10.0,
                    60,
                    LocalDate.now(),
                    "Paris",
                    4,
                    List.of(1L, 2L));

            assertThat(r.inviteIds()).containsExactly(1L, 2L);
        }
    }

    @Nested
    class ModifierChallengeRequestTest {

        @Test
        void accessors() {
            LocalDate d0 = LocalDate.of(2026, 3, 1);
            LocalDate d1 = LocalDate.of(2026, 4, 1);
            ModifierChallengeRequest r = new ModifierChallengeRequest("Ch", d0, d1);

            assertThat(r.nom()).isEqualTo("Ch");
            assertThat(r.dateDebut()).isEqualTo(d0);
            assertThat(r.dateFin()).isEqualTo(d1);
        }
    }

    @Nested
    class ModifierEvenementRequestTest {

        @Test
        void accessors() {
            LocalDateTime dt = LocalDateTime.of(2026, 5, 1, 12, 0);
            ModifierEvenementRequest r = new ModifierEvenementRequest("Ev", "desc", dt);

            assertThat(r.nom()).isEqualTo("Ev");
            assertThat(r.description()).isEqualTo("desc");
            assertThat(r.date()).isEqualTo(dt);
        }
    }

    @Nested
    class NotificationItemTest {

        @Test
        void accessors() {
            LocalDateTime dt = LocalDateTime.of(2026, 6, 1, 8, 0);
            NotificationItem n = new NotificationItem(
                    NotificationType.REACTION,
                    "t",
                    "d",
                    dt,
                    "/lien",
                    9L,
                    "/photo.jpg",
                    true);

            assertThat(n.type()).isEqualTo(NotificationType.REACTION);
            assertThat(n.titre()).isEqualTo("t");
            assertThat(n.detail()).isEqualTo("d");
            assertThat(n.dateTri()).isEqualTo(dt);
            assertThat(n.lien()).isEqualTo("/lien");
            assertThat(n.referenceId()).isEqualTo(9L);
            assertThat(n.photoProfilUrl()).isEqualTo("/photo.jpg");
            assertThat(n.nonLue()).isTrue();
        }
    }
}
