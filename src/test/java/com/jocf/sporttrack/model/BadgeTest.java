package com.jocf.sporttrack.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BadgeTest {

    @Test
    void shouldCreateBadgeWithNoArgsConstructorAndSetters() {
        Badge badge = new Badge();

        badge.setId(1L);
        badge.setCode("PREMIER_PAS");
        badge.setNom("Premier pas");
        badge.setPhoto("https://example.com/badge.png");
        badge.setDescription("Badge débloqué après la première activité");

        assertNotNull(badge);
        assertEquals(1L, badge.getId());
        assertEquals("PREMIER_PAS", badge.getCode());
        assertEquals("Premier pas", badge.getNom());
        assertEquals("https://example.com/badge.png", badge.getPhoto());
        assertEquals("Badge débloqué après la première activité", badge.getDescription());
    }

    @Test
    void shouldCreateBadgeWithConstructorIdCodeNomPhoto() {
        Badge badge = new Badge(
                2L,
                "RUNNER",
                "Runner",
                "https://example.com/runner.png"
        );

        assertNotNull(badge);
        assertEquals(2L, badge.getId());
        assertEquals("RUNNER", badge.getCode());
        assertEquals("Runner", badge.getNom());
        assertEquals("https://example.com/runner.png", badge.getPhoto());
        assertNull(badge.getDescription());
    }

    @Test
    void shouldCreateBadgeWithConstructorCodeNomPhoto() {
        Badge badge = new Badge(
                "CYCLISTE",
                "Cycliste",
                "https://example.com/cycliste.png"
        );

        assertNotNull(badge);
        assertNull(badge.getId());
        assertEquals("CYCLISTE", badge.getCode());
        assertEquals("Cycliste", badge.getNom());
        assertEquals("https://example.com/cycliste.png", badge.getPhoto());
        assertNull(badge.getDescription());
    }

    @Test
    void shouldCreateBadgeWithConstructorCodeNomPhotoDescription() {
        Badge badge = new Badge(
                "MARATHON",
                "Marathon",
                "https://example.com/marathon.png",
                "Terminer un marathon"
        );

        assertNotNull(badge);
        assertNull(badge.getId());
        assertEquals("MARATHON", badge.getCode());
        assertEquals("Marathon", badge.getNom());
        assertEquals("https://example.com/marathon.png", badge.getPhoto());
        assertEquals("Terminer un marathon", badge.getDescription());
    }

    @Test
    void shouldBeEqualWhenAllFieldsAreSame() {
        Badge badge1 = new Badge();
        badge1.setId(1L);
        badge1.setCode("PREMIER_PAS");
        badge1.setNom("Premier pas");
        badge1.setPhoto("https://example.com/badge.png");
        badge1.setDescription("Description");

        Badge badge2 = new Badge();
        badge2.setId(1L);
        badge2.setCode("PREMIER_PAS");
        badge2.setNom("Premier pas");
        badge2.setPhoto("https://example.com/badge.png");
        badge2.setDescription("Description");

        assertEquals(badge1, badge2);
        assertEquals(badge1.hashCode(), badge2.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenFieldsDiffer() {
        Badge badge1 = new Badge();
        badge1.setId(1L);
        badge1.setCode("PREMIER_PAS");
        badge1.setNom("Premier pas");
        badge1.setPhoto("https://example.com/badge.png");
        badge1.setDescription("Description");

        Badge badge2 = new Badge();
        badge2.setId(2L);
        badge2.setCode("AUTRE_BADGE");
        badge2.setNom("Autre badge");
        badge2.setPhoto("https://example.com/autre.png");
        badge2.setDescription("Autre description");

        assertNotEquals(badge1, badge2);
    }

    @Test
    void shouldNotBeEqualToNullOrDifferentClass() {
        Badge badge = new Badge();
        badge.setId(1L);
        badge.setCode("PREMIER_PAS");

        assertNotEquals(badge, null);
        assertNotEquals(badge, "badge");
    }

    @Test
    void shouldBeEqualToItself() {
        Badge badge = new Badge();
        badge.setId(1L);
        badge.setCode("PREMIER_PAS");

        assertEquals(badge, badge);
    }

    @Test
    void shouldHandleNullFieldsInEqualsAndHashCode() {
        Badge badge1 = new Badge();
        Badge badge2 = new Badge();

        assertEquals(badge1, badge2);
        assertEquals(badge1.hashCode(), badge2.hashCode());
    }
}