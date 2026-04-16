package com.jocf.sporttrack.model;

import com.jocf.sporttrack.enumeration.TypeCommentaire;
import com.jocf.sporttrack.enumeration.TypeSport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;

class ActiviteTest {

    private Activite activite;
    private Utilisateur auteur1;
    private Utilisateur auteur2;
    private Utilisateur auteur3;

    @BeforeEach
    void setUp() {
        auteur1 = creerUtilisateur(1L, "Jean", "Dupont");
        auteur2 = creerUtilisateur(2L, "Marie", "Martin");
        auteur3 = creerUtilisateur(3L, "Paul", "Bernard");

        activite = Activite.builder()
                .id(100L)
                .nom("Course du dimanche")
                .typeSport(TypeSport.COURSE)
                .date(LocalDate.now())
                .utilisateur(auteur1)
                .commentaires(new ArrayList<>())
                .invites(new ArrayList<>())
                .build();
    }

    @Test
    void getCommentairesMessages_doitRetournerSeulementLesCommentairesDeTypeMessage() {
        Commentaire message1 = creerCommentaire(1L, TypeCommentaire.MESSAGE, "Bravo !", auteur2);
        Commentaire reaction1 = creerCommentaire(2L, TypeCommentaire.REACTION, "🔥", auteur3);
        Commentaire message2 = creerCommentaire(3L, TypeCommentaire.MESSAGE, "Belle perf", auteur1);

        activite.getCommentaires().add(message1);
        activite.getCommentaires().add(reaction1);
        activite.getCommentaires().add(message2);

        List<Commentaire> result = activite.getCommentairesMessages();

        assertEquals(2, result.size());
        assertTrue(result.contains(message1));
        assertTrue(result.contains(message2));
        assertFalse(result.contains(reaction1));
    }

    @Test
    void getCommentairesMessages_doitRetournerListeVideSiAucunMessage() {
        activite.getCommentaires().add(creerCommentaire(1L, TypeCommentaire.REACTION, "🔥", auteur1));
        activite.getCommentaires().add(creerCommentaire(2L, TypeCommentaire.REACTION, "👏", auteur2));

        List<Commentaire> result = activite.getCommentairesMessages();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getReactionsGroupees_doitRegrouperParEmojiEnConservantOrdreApparition() {
        activite.getCommentaires().add(creerCommentaire(1L, TypeCommentaire.REACTION, "🔥", auteur1));
        activite.getCommentaires().add(creerCommentaire(2L, TypeCommentaire.REACTION, "👏", auteur2));
        activite.getCommentaires().add(creerCommentaire(3L, TypeCommentaire.REACTION, "🔥", auteur3));
        activite.getCommentaires().add(creerCommentaire(4L, TypeCommentaire.MESSAGE, "Top", auteur2));

        List<ReactionGroupee> result = activite.getReactionsGroupees();

        assertEquals(2, result.size());

        ReactionGroupee premiere = result.get(0);
        assertEquals("🔥", premiere.emoji());
        assertEquals(2, premiere.nombre());
        assertEquals("Jean Dupont, Paul Bernard", premiere.nomsDesReacteurs());

        ReactionGroupee deuxieme = result.get(1);
        assertEquals("👏", deuxieme.emoji());
        assertEquals(1, deuxieme.nombre());
        assertEquals("Marie Martin", deuxieme.nomsDesReacteurs());
    }

    @Test
    void getReactionsGroupees_doitIgnorerLesReactionsSansAuteur() {
        activite.getCommentaires().add(creerCommentaire(1L, TypeCommentaire.REACTION, "🔥", null));
        activite.getCommentaires().add(creerCommentaire(2L, TypeCommentaire.REACTION, "🔥", auteur1));

        List<ReactionGroupee> result = activite.getReactionsGroupees();

        assertEquals(1, result.size());
        assertEquals("🔥", result.get(0).emoji());
        assertEquals(1, result.get(0).nombre());
        assertEquals("Jean Dupont", result.get(0).nomsDesReacteurs());
    }

    @Test
    void getReactionsGroupeesAffichees_doitRetournerToutesSiInferieurOuEgalALaLimite() {
        ajouterReactionsDistinctes(5);

        List<ReactionGroupee> result = activite.getReactionsGroupeesAffichees();

        assertEquals(5, result.size());
        assertEquals(0, activite.getReactionsGroupeesMasqueesCount());
    }

    @Test
    void getReactionsGroupeesAffichees_doitLimiterALaValeurMaximale() {
        ajouterReactionsDistinctes(7);

        List<ReactionGroupee> result = activite.getReactionsGroupeesAffichees();

        assertEquals(5, result.size());
        assertEquals("emoji1", result.get(0).emoji());
        assertEquals("emoji5", result.get(4).emoji());
    }

    @Test
    void getReactionsGroupeesMasqueesCount_doitRetournerNombreMasque() {
        ajouterReactionsDistinctes(7);

        int result = activite.getReactionsGroupeesMasqueesCount();

        assertEquals(2, result);
    }

    @Test
    void getReactionsGroupeesMasqueesCount_doitRetournerZeroSiPasDeDepassement() {
        ajouterReactionsDistinctes(3);

        int result = activite.getReactionsGroupeesMasqueesCount();

        assertEquals(0, result);
    }

    @Test
    void utilisateurAEmitReactionAvecEmoji_doitRetournerTrueSiReactionExiste() {
        activite.getCommentaires().add(creerCommentaire(10L, TypeCommentaire.REACTION, "🔥", auteur2));

        boolean result = activite.utilisateurAEmitReactionAvecEmoji(2L, "🔥");

        assertTrue(result);
    }

    @Test
    void utilisateurAEmitReactionAvecEmoji_doitRetournerFalseSiReactionAbsente() {
        activite.getCommentaires().add(creerCommentaire(10L, TypeCommentaire.REACTION, "🔥", auteur2));

        boolean result = activite.utilisateurAEmitReactionAvecEmoji(2L, "👏");

        assertFalse(result);
    }

    @Test
    void getIdCommentaireReactionUtilisateur_doitRetournerIdDuCommentaireTrouve() {
        activite.getCommentaires().add(creerCommentaire(55L, TypeCommentaire.REACTION, "🔥", auteur2));
        activite.getCommentaires().add(creerCommentaire(56L, TypeCommentaire.REACTION, "👏", auteur2));

        Long result = activite.getIdCommentaireReactionUtilisateur(2L, "👏");

        assertEquals(56L, result);
    }

    @Test
    void getIdCommentaireReactionUtilisateur_doitRetournerNullSiUtilisateurIdNull() {
        activite.getCommentaires().add(creerCommentaire(55L, TypeCommentaire.REACTION, "🔥", auteur2));

        Long result = activite.getIdCommentaireReactionUtilisateur(null, "🔥");

        assertNull(result);
    }

    @Test
    void getIdCommentaireReactionUtilisateur_doitRetournerNullSiEmojiNull() {
        activite.getCommentaires().add(creerCommentaire(55L, TypeCommentaire.REACTION, "🔥", auteur2));

        Long result = activite.getIdCommentaireReactionUtilisateur(2L, null);

        assertNull(result);
    }

    @Test
    void getIdCommentaireReactionUtilisateur_doitRetournerNullSiAucuneReactionCorrespondante() {
        activite.getCommentaires().add(creerCommentaire(55L, TypeCommentaire.REACTION, "🔥", auteur2));

        Long result = activite.getIdCommentaireReactionUtilisateur(3L, "🔥");

        assertNull(result);
    }

    @Test
    void getReactionsParEmoji_doitRetournerUneMapAvecLesComptages() {
        activite.getCommentaires().add(creerCommentaire(1L, TypeCommentaire.REACTION, "🔥", auteur1));
        activite.getCommentaires().add(creerCommentaire(2L, TypeCommentaire.REACTION, "🔥", auteur2));
        activite.getCommentaires().add(creerCommentaire(3L, TypeCommentaire.REACTION, "👏", auteur3));

        Map<String, Long> result = activite.getReactionsParEmoji();

        assertEquals(2, result.size());
        assertEquals(2L, result.get("🔥"));
        assertEquals(1L, result.get("👏"));
    }

    @Test
    void getInvitesAffiches_doitRetournerListeVideSiInvitesNull() {
        activite.setInvites(null);

        List<Utilisateur> result = activite.getInvitesAffiches();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getInvitesAffiches_doitRetournerListeVideSiAucunInvite() {
        activite.setInvites(new ArrayList<>());

        List<Utilisateur> result = activite.getInvitesAffiches();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getInvitesAffiches_doitRetournerTousLesInvitesSiInferieurOuEgalALaLimite() {
        activite.setInvites(List.of(auteur1, auteur2, auteur3));

        List<Utilisateur> result = activite.getInvitesAffiches();

        assertEquals(3, result.size());
        assertEquals(0, activite.getInvitesMasquesCount());
    }

    @Test
    void getInvitesAffiches_doitLimiterALaTailleMaximale() {
        Utilisateur u4 = creerUtilisateur(4L, "Luc", "Petit");
        Utilisateur u5 = creerUtilisateur(5L, "Anna", "Moreau");

        activite.setInvites(List.of(auteur1, auteur2, auteur3, u4, u5));

        List<Utilisateur> result = activite.getInvitesAffiches();

        assertEquals(3, result.size());
        assertEquals(auteur1, result.get(0));
        assertEquals(auteur2, result.get(1));
        assertEquals(auteur3, result.get(2));
    }

    @Test
    void getInvitesMasquesCount_doitRetournerLeNombreMasque() {
        Utilisateur u4 = creerUtilisateur(4L, "Luc", "Petit");
        Utilisateur u5 = creerUtilisateur(5L, "Anna", "Moreau");

        activite.setInvites(List.of(auteur1, auteur2, auteur3, u4, u5));

        int result = activite.getInvitesMasquesCount();

        assertEquals(2, result);
    }

    @Test
    void getInvitesMasquesCount_doitRetournerZeroSiInvitesNull() {
        activite.setInvites(null);

        int result = activite.getInvitesMasquesCount();

        assertEquals(0, result);
    }

    @Test
    void getLimiteReactionsAffichees_doitRetourner5() {
        assertEquals(5, Activite.getLimiteReactionsAffichees());
    }

    private Utilisateur creerUtilisateur(Long id, String prenom, String nom) {
        Utilisateur u = new Utilisateur();
        u.setId(id);
        u.setPrenom(prenom);
        u.setNom(nom);
        return u;
    }

    private Commentaire creerCommentaire(Long id, TypeCommentaire type, String message, Utilisateur auteur) {
        Commentaire c = new Commentaire();
        c.setId(id);
        c.setType(type);
        c.setMessage(message);
        c.setAuteur(auteur);
        c.setActivite(activite);
        c.setDateCreation(LocalDateTime.now());
        return c;
    }

    private void ajouterReactionsDistinctes(int nombre) {
        for (int i = 1; i <= nombre; i++) {
            Utilisateur auteur = creerUtilisateur((long) i, "Prenom" + i, "Nom" + i);
            activite.getCommentaires().add(
                    creerCommentaire((long) i, TypeCommentaire.REACTION, "emoji" + i, auteur)
            );
        }
    }

    @Test
    void getReactionsGroupeesAffichees_retourneToutesSousLaLimite() {
        Utilisateur u = Utilisateur.builder().id(1L).prenom("X").nom("Y").build();
        Activite activiteTest = Activite.builder()
                .nom("test")
                .typeSport(TypeSport.BOXE)
                .date(LocalDate.now())
                .utilisateur(u)
                .build();
        activiteTest.getCommentaires().add(
                Commentaire.builder().id(1L).type(TypeCommentaire.REACTION)
                        .message("👍").auteur(u).build());

        assertThat(activiteTest.getReactionsGroupeesAffichees()).hasSize(1);
    }

    @Test
    void getReactionsGroupeesMasqueesCount_retourne0SiMoinsQue5() {
        Utilisateur u = Utilisateur.builder().id(1L).prenom("A").nom("B").build();
        Activite activiteTest = Activite.builder()
                .nom("test")
                .typeSport(TypeSport.TENNIS)
                .date(LocalDate.now())
                .utilisateur(u)
                .build();
        activiteTest.getCommentaires().add(
                Commentaire.builder().id(1L).type(TypeCommentaire.REACTION)
                        .message("👍").auteur(u).build());

        assertThat(activiteTest.getReactionsGroupeesMasqueesCount()).isZero();
    }

    @Test
    void utilisateurAEmitReactionAvecEmoji_trouveReactionExistante() {
        Utilisateur u = Utilisateur.builder().id(1L).prenom("A").nom("B").build();
        Commentaire reaction = Commentaire.builder()
                .id(10L).type(TypeCommentaire.REACTION).message("🔥").auteur(u).build();

        Activite activiteTest = Activite.builder()
                .nom("test").typeSport(TypeSport.CYCLISME)
                .date(LocalDate.now()).utilisateur(u).build();
        activiteTest.getCommentaires().add(reaction);

        assertThat(activiteTest.utilisateurAEmitReactionAvecEmoji(1L, "🔥")).isTrue();
        assertThat(activiteTest.utilisateurAEmitReactionAvecEmoji(1L, "💀")).isFalse();
        assertThat(activiteTest.utilisateurAEmitReactionAvecEmoji(99L, "🔥")).isFalse();
    }

    @Test
    void utilisateurAEmitReactionAvecEmoji_avecIdOuEmojiNull_retourneFalse() {
        Activite activiteTest = Activite.builder()
                .nom("t").typeSport(TypeSport.YOGA)
                .date(LocalDate.now())
                .utilisateur(Utilisateur.builder().id(1L).build())
                .build();

        assertThat(activiteTest.utilisateurAEmitReactionAvecEmoji(null, "👍")).isFalse();
        assertThat(activiteTest.utilisateurAEmitReactionAvecEmoji(1L, null)).isFalse();
    }

    @Test
    void getIdCommentaireReactionUtilisateur_retourneLId() {
        Utilisateur u = Utilisateur.builder().id(5L).prenom("A").nom("B").build();
        Commentaire reaction = Commentaire.builder()
                .id(42L).type(TypeCommentaire.REACTION).message("❤️").auteur(u).build();

        Activite activiteTest = Activite.builder()
                .nom("t").typeSport(TypeSport.NATATION)
                .date(LocalDate.now()).utilisateur(u).build();
        activiteTest.getCommentaires().add(reaction);

        assertThat(activiteTest.getIdCommentaireReactionUtilisateur(5L, "❤️")).isEqualTo(42L);
        assertThat(activiteTest.getIdCommentaireReactionUtilisateur(5L, "👍")).isNull();
    }

    @Test
    void getReactionsParEmoji_retourneMapAvecCounts() {
        Utilisateur u1 = Utilisateur.builder().id(1L).prenom("A").nom("B").build();
        Utilisateur u2 = Utilisateur.builder().id(2L).prenom("C").nom("D").build();

        Activite activiteTest = Activite.builder()
                .nom("t").typeSport(TypeSport.FOOTBALL)
                .date(LocalDate.now()).utilisateur(u1).build();
        activiteTest.getCommentaires().add(
                Commentaire.builder().id(1L).type(TypeCommentaire.REACTION).message("👍").auteur(u1).build());
        activiteTest.getCommentaires().add(
                Commentaire.builder().id(2L).type(TypeCommentaire.REACTION).message("👍").auteur(u2).build());
        activiteTest.getCommentaires().add(
                Commentaire.builder().id(3L).type(TypeCommentaire.REACTION).message("🔥").auteur(u1).build());

        var map = activiteTest.getReactionsParEmoji();

        assertThat(map).containsEntry("👍", 2L).containsEntry("🔥", 1L);
    }

    @Test
    void getInvitesAffiches_etMasques_couvrentLesBranches() {
        Utilisateur hote = Utilisateur.builder().id(1L).prenom("H").nom("O").build();
        Utilisateur i1 = Utilisateur.builder().id(2L).prenom("A").nom("A").build();
        Utilisateur i2 = Utilisateur.builder().id(3L).prenom("B").nom("B").build();
        Utilisateur i3 = Utilisateur.builder().id(4L).prenom("C").nom("C").build();
        Utilisateur i4 = Utilisateur.builder().id(5L).prenom("D").nom("D").build();

        Activite vide = Activite.builder()
                .nom("vide")
                .typeSport(TypeSport.YOGA)
                .date(LocalDate.now())
                .utilisateur(hote)
                .build();
        assertThat(vide.getInvitesAffiches()).isEmpty();
        assertThat(vide.getInvitesMasquesCount()).isZero();

        Activite activiteTest = Activite.builder()
                .nom("invite")
                .typeSport(TypeSport.YOGA)
                .date(LocalDate.now())
                .utilisateur(hote)
                .invites(List.of(i1, i2, i3, i4))
                .build();

        assertThat(activiteTest.getInvitesAffiches()).containsExactly(i1, i2, i3);
        assertThat(activiteTest.getInvitesMasquesCount()).isEqualTo(1);
    }

    @Test
    void getInvitesAffiches_retourneLaListeOriginaleSousLaLimite() {
        Utilisateur hote = Utilisateur.builder().id(1L).prenom("H").nom("O").build();
        Utilisateur i1 = Utilisateur.builder().id(2L).prenom("A").nom("A").build();
        Utilisateur i2 = Utilisateur.builder().id(3L).prenom("B").nom("B").build();
        Utilisateur i3 = Utilisateur.builder().id(4L).prenom("C").nom("C").build();

        Activite activiteTest = Activite.builder()
                .nom("invite")
                .typeSport(TypeSport.YOGA)
                .date(LocalDate.now())
                .utilisateur(hote)
                .invites(List.of(i1, i2, i3))
                .build();

        assertThat(activiteTest.getInvitesAffiches()).containsExactly(i1, i2, i3);
        assertThat(activiteTest.getInvitesMasquesCount()).isZero();
    }

    @Test
    void getInvitesAffiches_retourneListeVideQuandInvitesNull() {
        Activite activiteTest = Activite.builder()
                .nom("invite")
                .typeSport(TypeSport.YOGA)
                .date(LocalDate.now())
                .utilisateur(Utilisateur.builder().id(1L).build())
                .invites(null)
                .build();

        assertThat(activiteTest.getInvitesAffiches()).isEmpty();
        assertThat(activiteTest.getInvitesMasquesCount()).isZero();
    }
}
