package com.jocf.sporttrack.model;

import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ChallengeTest {

    @Test
    void shouldCreateChallengeWithNoArgsConstructorAndSetters() {
        Utilisateur organisateur = new Utilisateur();
        organisateur.setId(1L);

        Utilisateur participant1 = new Utilisateur();
        participant1.setId(2L);

        Utilisateur participant2 = new Utilisateur();
        participant2.setId(3L);

        Set<Utilisateur> participants = new HashSet<>();
        participants.add(participant1);
        participants.add(participant2);

        Date dateDebut = Date.valueOf("2026-04-01");
        Date dateFin = Date.valueOf("2026-04-30");

        Challenge challenge = new Challenge();
        challenge.setId(10L);
        challenge.setNom("30 jours de course");
        challenge.setDateDebut(dateDebut);
        challenge.setDateFin(dateFin);
        challenge.setObjectifJour("Courir 5 km par jour");
        challenge.setOrganisateur(organisateur);
        challenge.setParticipants(participants);

        assertNotNull(challenge);
        assertEquals(10L, challenge.getId());
        assertEquals("30 jours de course", challenge.getNom());
        assertEquals(dateDebut, challenge.getDateDebut());
        assertEquals(dateFin, challenge.getDateFin());
        assertEquals("Courir 5 km par jour", challenge.getObjectifJour());
        assertEquals(organisateur, challenge.getOrganisateur());
        assertEquals(2, challenge.getParticipants().size());
        assertTrue(challenge.getParticipants().contains(participant1));
        assertTrue(challenge.getParticipants().contains(participant2));
    }

    @Test
    void shouldCreateChallengeWithBuilder() {
        Utilisateur organisateur = new Utilisateur();
        organisateur.setId(1L);

        Utilisateur participant = new Utilisateur();
        participant.setId(2L);

        Date dateDebut = Date.valueOf("2026-05-01");
        Date dateFin = Date.valueOf("2026-05-31");

        Challenge challenge = Challenge.builder()
                .id(20L)
                .nom("Challenge vélo")
                .dateDebut(dateDebut)
                .dateFin(dateFin)
                .objectifJour("Faire 20 km")
                .organisateur(organisateur)
                .build();

        challenge.getParticipants().add(participant);

        assertNotNull(challenge);
        assertEquals(20L, challenge.getId());
        assertEquals("Challenge vélo", challenge.getNom());
        assertEquals(dateDebut, challenge.getDateDebut());
        assertEquals(dateFin, challenge.getDateFin());
        assertEquals("Faire 20 km", challenge.getObjectifJour());
        assertEquals(organisateur, challenge.getOrganisateur());
        assertEquals(1, challenge.getParticipants().size());
        assertTrue(challenge.getParticipants().contains(participant));
    }

    @Test
    void shouldCreateChallengeWithAllArgsConstructor() {
        Utilisateur organisateur = new Utilisateur();
        organisateur.setId(1L);

        Utilisateur participant = new Utilisateur();
        participant.setId(2L);

        Set<Utilisateur> participants = new HashSet<>();
        participants.add(participant);

        Date dateDebut = Date.valueOf("2026-06-01");
        Date dateFin = Date.valueOf("2026-06-15");

        Challenge challenge = new Challenge(
                30L,
                "Challenge natation",
                dateDebut,
                dateFin,
                "Nager 1 km",
                organisateur,
                participants
        );

        assertNotNull(challenge);
        assertEquals(30L, challenge.getId());
        assertEquals("Challenge natation", challenge.getNom());
        assertEquals(dateDebut, challenge.getDateDebut());
        assertEquals(dateFin, challenge.getDateFin());
        assertEquals("Nager 1 km", challenge.getObjectifJour());
        assertEquals(organisateur, challenge.getOrganisateur());
        assertEquals(participants, challenge.getParticipants());
    }

    @Test
    void shouldInitializeParticipantsWithEmptySetByDefaultInBuilder() {
        Utilisateur organisateur = new Utilisateur();
        organisateur.setId(1L);

        Challenge challenge = Challenge.builder()
                .id(40L)
                .nom("Challenge marche")
                .dateDebut(Date.valueOf("2026-07-01"))
                .dateFin(Date.valueOf("2026-07-10"))
                .objectifJour("Marcher 10 000 pas")
                .organisateur(organisateur)
                .build();

        assertNotNull(challenge.getParticipants());
        assertTrue(challenge.getParticipants().isEmpty());
    }

    @Test
    void shouldValidateEqualsAndHashCodeForSameValues() {
        Utilisateur organisateur = new Utilisateur();
        organisateur.setId(1L);

        Set<Utilisateur> participants = new HashSet<>();

        Date dateDebut = Date.valueOf("2026-08-01");
        Date dateFin = Date.valueOf("2026-08-31");

        Challenge challenge1 = Challenge.builder()
                .id(50L)
                .nom("Challenge fitness")
                .dateDebut(dateDebut)
                .dateFin(dateFin)
                .objectifJour("30 min d'exercice")
                .organisateur(organisateur)
                .participants(participants)
                .build();

        Challenge challenge2 = Challenge.builder()
                .id(50L)
                .nom("Challenge fitness")
                .dateDebut(dateDebut)
                .dateFin(dateFin)
                .objectifJour("30 min d'exercice")
                .organisateur(organisateur)
                .participants(participants)
                .build();

        assertEquals(challenge1, challenge2);
        assertEquals(challenge1.hashCode(), challenge2.hashCode());
    }

    @Test
    void shouldContainNomInToString() {
        Utilisateur organisateur = new Utilisateur();
        organisateur.setId(1L);

        Challenge challenge = Challenge.builder()
                .id(60L)
                .nom("Challenge yoga")
                .dateDebut(Date.valueOf("2026-09-01"))
                .dateFin(Date.valueOf("2026-09-30"))
                .objectifJour("20 min de yoga")
                .organisateur(organisateur)
                .build();

        String result = challenge.toString();

        assertNotNull(result);
        assertTrue(result.contains("Challenge yoga"));
    }
}