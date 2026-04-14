package com.jocf.sporttrack.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BadgeTest {

    @Test
    void constructeursEtAccesseurs_fonctionnent() {
        Badge badge = new Badge(1L, "PREMIER_PAS", "Premier pas", "/img.png");
        badge.setDescription("Desc");

        assertThat(badge.getId()).isEqualTo(1L);
        assertThat(badge.getCode()).isEqualTo("PREMIER_PAS");
        assertThat(badge.getNom()).isEqualTo("Premier pas");
        assertThat(badge.getPhoto()).isEqualTo("/img.png");
        assertThat(badge.getDescription()).isEqualTo("Desc");
    }

    @Test
    void equalsEtHashCode_dependantDesChamps() {
        Badge a = new Badge("PREMIER_PAS", "Premier pas", "/img.png", "Desc");
        Badge b = new Badge("PREMIER_PAS", "Premier pas", "/img.png", "Desc");

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}
