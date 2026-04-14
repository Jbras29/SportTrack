package com.jocf.sporttrack.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AnnonceTest {

    @Test
    void shouldCreateAnnonceWithBuilder() {
        LocalDateTime now = LocalDateTime.now();
        Evenement evenement = new Evenement();
        evenement.setId(1L);

        Annonce annonce = Annonce.builder()
                .id(10L)
                .message("Match reporté à 18h")
                .date(now)
                .evenement(evenement)
                .build();

        assertNotNull(annonce);
        assertEquals(10L, annonce.getId());
        assertEquals("Match reporté à 18h", annonce.getMessage());
        assertEquals(now, annonce.getDate());
        assertEquals(evenement, annonce.getEvenement());
    }

    @Test
    void shouldCreateAnnonceWithNoArgsConstructorAndSetters() {
        LocalDateTime now = LocalDateTime.now();
        Evenement evenement = new Evenement();
        evenement.setId(2L);

        Annonce annonce = new Annonce();
        annonce.setId(20L);
        annonce.setMessage("Entraînement annulé");
        annonce.setDate(now);
        annonce.setEvenement(evenement);

        assertNotNull(annonce);
        assertEquals(20L, annonce.getId());
        assertEquals("Entraînement annulé", annonce.getMessage());
        assertEquals(now, annonce.getDate());
        assertEquals(evenement, annonce.getEvenement());
    }

    @Test
    void shouldCreateAnnonceWithAllArgsConstructor() {
        LocalDateTime now = LocalDateTime.now();
        Evenement evenement = new Evenement();
        evenement.setId(3L);

        Annonce annonce = new Annonce(
                30L,
                "Nouvelle annonce",
                now,
                evenement
        );

        assertNotNull(annonce);
        assertEquals(30L, annonce.getId());
        assertEquals("Nouvelle annonce", annonce.getMessage());
        assertEquals(now, annonce.getDate());
        assertEquals(evenement, annonce.getEvenement());
    }

    @Test
    void shouldValidateEqualsAndHashCodeForSameValues() {
        LocalDateTime now = LocalDateTime.now();
        Evenement evenement = new Evenement();
        evenement.setId(4L);

        Annonce annonce1 = Annonce.builder()
                .id(1L)
                .message("Annonce identique")
                .date(now)
                .evenement(evenement)
                .build();

        Annonce annonce2 = Annonce.builder()
                .id(1L)
                .message("Annonce identique")
                .date(now)
                .evenement(evenement)
                .build();

        assertEquals(annonce1, annonce2);
        assertEquals(annonce1.hashCode(), annonce2.hashCode());
    }

    @Test
    void shouldContainFieldsInToString() {
        LocalDateTime now = LocalDateTime.now();
        Evenement evenement = new Evenement();
        evenement.setId(5L);

        Annonce annonce = Annonce.builder()
                .id(50L)
                .message("Test toString")
                .date(now)
                .evenement(evenement)
                .build();

        String result = annonce.toString();

        assertNotNull(result);
        assertTrue(result.contains("Test toString"));
    }
}