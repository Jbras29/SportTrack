package com.jocf.sporttrack.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
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

        assertThat(badge1).isEqualTo(badge2).hasSameHashCodeAs(badge2);
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
    void shouldNotBeEqualWhenIdDiffers() {
        Badge badge1 = badgeBase();
        Badge badge2 = badgeBase();
        badge2.setId(2L);

        assertThat(badge1).isNotEqualTo(badge2);
    }

    @Test
    void shouldNotBeEqualWhenIdIsNullOnOneSide() {
        Badge badge1 = new Badge();
        badge1.setCode("PREMIER_PAS");
        Badge badge2 = badgeBase();

        assertThat(badge1.equals(badge2)).isFalse();
    }

    @Test
    void shouldNotBeEqualToDifferentClassViaEquals() {
        Badge badge = badgeBase();

        assertThat(badge.equals("badge")).isFalse();
    }

    @Test
    void shouldNotBeEqualToNullViaEquals() {
        Badge badge = badgeBase();

        assertThat(badge.equals(null)).isFalse();
    }

    @Test
    void shouldNotBeEqualWhenCodeDiffers() {
        Badge badge1 = badgeBase();
        Badge badge2 = badgeBase();
        badge2.setCode("AUTRE");

        assertThat(badge1).isNotEqualTo(badge2);
    }

    @Test
    void shouldNotBeEqualWhenCodeIsNullOnOneSide() {
        Badge badge1 = badgeBase();
        badge1.setCode(null);
        Badge badge2 = badgeBase();
        badge2.setCode("AUTRE");

        assertThat(badge1.equals(badge2)).isFalse();
    }

    @Test
    void shouldNotBeEqualWhenNomDiffers() {
        Badge badge1 = badgeBase();
        Badge badge2 = badgeBase();
        badge2.setNom("Autre nom");

        assertThat(badge1).isNotEqualTo(badge2);
    }

    @Test
    void shouldNotBeEqualWhenNomIsNullOnOneSide() {
        Badge badge1 = badgeBase();
        badge1.setNom(null);
        Badge badge2 = badgeBase();
        badge2.setNom("Autre nom");

        assertThat(badge1.equals(badge2)).isFalse();
    }

    @Test
    void shouldNotBeEqualWhenPhotoDiffers() {
        Badge badge1 = badgeBase();
        Badge badge2 = badgeBase();
        badge2.setPhoto("https://example.com/autre.png");

        assertThat(badge1).isNotEqualTo(badge2);
    }

    @Test
    void shouldNotBeEqualWhenPhotoIsNullOnOneSide() {
        Badge badge1 = badgeBase();
        badge1.setPhoto(null);
        Badge badge2 = badgeBase();
        badge2.setPhoto("https://example.com/autre.png");

        assertThat(badge1.equals(badge2)).isFalse();
    }

    @Test
    void shouldNotBeEqualWhenDescriptionDiffers() {
        Badge badge1 = badgeBase();
        Badge badge2 = badgeBase();
        badge2.setDescription("Autre description");

        assertThat(badge1).isNotEqualTo(badge2);
    }

    @Test
    void shouldNotBeEqualWhenDescriptionIsNullOnOneSide() {
        Badge badge1 = badgeBase();
        badge1.setDescription(null);
        Badge badge2 = badgeBase();
        badge2.setDescription("Autre description");

        assertThat(badge1.equals(badge2)).isFalse();
    }

    @Test
    void shouldNotBeEqualToNullOrDifferentClass() {
        Badge badge = new Badge();
        badge.setId(1L);
        badge.setCode("PREMIER_PAS");

        assertNotEquals(null, badge);
        assertNotEquals("badge", badge);
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

        assertThat(badge1).isEqualTo(badge2).hasSameHashCodeAs(badge2);
    }

    private static Badge badgeBase() {
        Badge badge = new Badge();
        badge.setId(1L);
        badge.setCode("PREMIER_PAS");
        badge.setNom("Premier pas");
        badge.setPhoto("https://example.com/badge.png");
        badge.setDescription("Description");
        return badge;
    }
}
