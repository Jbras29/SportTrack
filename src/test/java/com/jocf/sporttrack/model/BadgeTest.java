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
    void constructeurSansIdEtSetters_fonctionnent() {
        Badge badge = new Badge("CINQ_K_STARTER", "5K Starter", "/5k.png");
        badge.setId(7L);
        badge.setDescription("Premier parcours de 5 km.");

        assertThat(badge.getId()).isEqualTo(7L);
        assertThat(badge.getCode()).isEqualTo("CINQ_K_STARTER");
        assertThat(badge.getNom()).isEqualTo("5K Starter");
        assertThat(badge.getPhoto()).isEqualTo("/5k.png");
        assertThat(badge.getDescription()).isEqualTo("Premier parcours de 5 km.");
    }

    @Test
    void equalsEtHashCode_dependantDesChamps() {
        Badge a = new Badge("PREMIER_PAS", "Premier pas", "/img.png", "Desc");
        Badge b = new Badge("PREMIER_PAS", "Premier pas", "/img.png", "Desc");

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void equalsRetourneFalsePourUnBadgeDifferent() {
        Badge a = new Badge("PREMIER_PAS", "Premier pas", "/img.png", "Desc");
        Badge b = new Badge("CINQ_K_STARTER", "5K Starter", "/5k.png", "Autre desc");

        assertThat(a).isNotEqualTo(b);
    }
}
