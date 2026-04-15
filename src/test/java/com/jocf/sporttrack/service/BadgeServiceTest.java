package com.jocf.sporttrack.service;

import com.jocf.sporttrack.model.Badge;
import com.jocf.sporttrack.enumeration.TypeUtilisateur;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.repository.BadgeRepository;
import com.jocf.sporttrack.repository.UtilisateurRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BadgeServiceTest {

    @Mock
    private BadgeRepository badgeRepository;

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @InjectMocks
    private BadgeService service;

    private Utilisateur utilisateur;
    private Badge badge;

    @BeforeEach
    void setUp() {
        badge = new Badge("PREMIER_PAS", "Premier pas", "/img.png", "desc");
        utilisateur = Utilisateur.builder()
                .id(1L)
                .nom("Doe")
                .prenom("Jane")
                .email("jane@test.com")
                .motdepasse("x")
                .typeUtilisateur(TypeUtilisateur.UTILISATEUR)
                .badges(new ArrayList<>())
                .build();
    }

    @Test
    void attribuerBadgeParCode_ajouteLeBadge() {
        when(badgeRepository.findByCode("PREMIER_PAS")).thenReturn(Optional.of(badge));
        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(utilisateur));

        service.attribuerBadgeParCode(1L, "PREMIER_PAS");

        assertThat(utilisateur.getBadges()).containsExactly(badge);
        verify(utilisateurRepository).save(utilisateur);
    }

    @Test
    void attribuerBadgeParCodeSiPresent_ignoreCodeInconnu() {
        when(badgeRepository.findByCode("INCONNU")).thenReturn(Optional.empty());

        service.attribuerBadgeParCodeSiPresent(1L, "INCONNU");

        verify(utilisateurRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void obtenirEtListerBadges_fonctionnent() {
        utilisateur.setBadges(new ArrayList<>(List.of(badge)));
        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(utilisateur));
        when(badgeRepository.findAll()).thenReturn(List.of(badge));

        assertThat(service.obtenirBadgesUtilisateur(1L)).containsExactly(badge);
        assertThat(service.listerTousLesBadges()).containsExactly(badge);
    }
}
