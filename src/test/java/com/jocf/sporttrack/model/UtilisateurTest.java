package com.jocf.sporttrack.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UtilisateurTest {

    @Test
    void constructeurSansArguments_creeUneInstance() {
        Utilisateur utilisateur = new Utilisateur();

        assertThat(utilisateur).isNotNull();
        assertThat(utilisateur.isComptePrive()).isFalse();
    }

    @Test
    void constructeurAvecTousLesArguments_initialiseLesChamps() {
        LocalDateTime derniereConsultation = LocalDateTime.of(2024, 6, 1, 12, 0);
        List<Utilisateur> amis = List.of();
        List<Utilisateur> demandes = List.of();
        List<PrefSportive> prefs = List.of();
        List<Activite> activites = List.of();
        List<Badge> badges = List.of();
        List<Evenement> organises = List.of();
        List<Evenement> participes = List.of();
        List<Message> envoyes = List.of();
        List<Message> recus = List.of();

        Utilisateur utilisateur = new Utilisateur(
                1L,
                "Dupont",
                "Jean",
                "jean@example.com",
                "motdepasse",
                "/uploads/profiles/1.jpg",
                derniereConsultation,
                TypeUtilisateur.ADMIN,
                true,
                "M",
                30,
                72.5,
                178.0,
                250,
                90,
                "Objectifs perso",
                NiveauPratiqueSportive.INTERMEDIAIRE,
                amis,
                demandes,
                prefs,
                activites,
                badges,
                organises,
                participes,
                envoyes,
                recus
        );

        assertThat(utilisateur.getId()).isEqualTo(1L);
        assertThat(utilisateur.getNom()).isEqualTo("Dupont");
        assertThat(utilisateur.getPrenom()).isEqualTo("Jean");
        assertThat(utilisateur.getEmail()).isEqualTo("jean@example.com");
        assertThat(utilisateur.getMotdepasse()).isEqualTo("motdepasse");
        assertThat(utilisateur.getPhotoProfil()).isEqualTo("/uploads/profiles/1.jpg");
        assertThat(utilisateur.getDerniereConsultationNotifications()).isEqualTo(derniereConsultation);
        assertThat(utilisateur.getTypeUtilisateur()).isEqualTo(TypeUtilisateur.ADMIN);
        assertThat(utilisateur.isComptePrive()).isTrue();
        assertThat(utilisateur.getSexe()).isEqualTo("M");
        assertThat(utilisateur.getAge()).isEqualTo(30);
        assertThat(utilisateur.getPoids()).isEqualTo(72.5);
        assertThat(utilisateur.getTaille()).isEqualTo(178.0);
        assertThat(utilisateur.getXp()).isEqualTo(250);
        assertThat(utilisateur.getHp()).isEqualTo(90);
        assertThat(utilisateur.getObjectifsPersonnels()).isEqualTo("Objectifs perso");
        assertThat(utilisateur.getNiveauPratiqueSportive()).isEqualTo(NiveauPratiqueSportive.INTERMEDIAIRE);
        assertThat(utilisateur.getAmis()).isSameAs(amis);
        assertThat(utilisateur.getDemandesAmisEnvoyees()).isSameAs(demandes);
        assertThat(utilisateur.getPrefSportives()).isSameAs(prefs);
        assertThat(utilisateur.getActivites()).isSameAs(activites);
        assertThat(utilisateur.getBadges()).isSameAs(badges);
        assertThat(utilisateur.getEvenementsOrganises()).isSameAs(organises);
        assertThat(utilisateur.getEvenementsParticipes()).isSameAs(participes);
        assertThat(utilisateur.getMessagesEnvoyes()).isSameAs(envoyes);
        assertThat(utilisateur.getMessagesRecus()).isSameAs(recus);
    }
}
