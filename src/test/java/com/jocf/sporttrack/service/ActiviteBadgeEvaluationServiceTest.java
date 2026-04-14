package com.jocf.sporttrack.service;

import com.jocf.sporttrack.model.Activite;
import com.jocf.sporttrack.model.TypeSport;
import com.jocf.sporttrack.model.TypeUtilisateur;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.repository.ActiviteRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActiviteBadgeEvaluationServiceTest {

    @Mock
    private ActiviteRepository activiteRepository;

    @Mock
    private BadgeService badgeService;

    @InjectMocks
    private ActiviteBadgeEvaluationService service;

    @Test
    void evaluerEtAttribuerBadges_ignoreActiviteInvalide() {
        service.evaluerEtAttribuerBadges(null);

        verify(badgeService, never()).attribuerBadgeParCodeSiPresent(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void evaluerEtAttribuerBadges_declenchePlusieursRegles() {
        Utilisateur user = Utilisateur.builder()
                .id(1L)
                .nom("Doe")
                .prenom("Jane")
                .email("jane@test.com")
                .motdepasse("x")
                .typeUtilisateur(TypeUtilisateur.UTILISATEUR)
                .build();
        Activite activite = Activite.builder()
                .id(10L)
                .utilisateur(user)
                .typeSport(TypeSport.COURSE)
                .distance(5000.0)
                .temps(70)
                .date(LocalDate.now())
                .location("Parc naturel")
                .build();
        when(activiteRepository.findByUtilisateur(user)).thenReturn(List.of(
                activite,
                Activite.builder().id(2L).utilisateur(user).typeSport(TypeSport.NATATION).distance(1000.0).temps(30).date(LocalDate.now().minusDays(1)).location("Lac").build(),
                Activite.builder().id(3L).utilisateur(user).typeSport(TypeSport.CYCLISME).distance(15000.0).temps(40).date(LocalDate.now().minusDays(2)).location("Ville").build()
        ));

        service.evaluerEtAttribuerBadges(activite);

        verify(badgeService).attribuerBadgeParCodeSiPresent(1L, "CINQ_K_STARTER");
        verify(badgeService).attribuerBadgeParCodeSiPresent(1L, "ENDURANCE_1H");
        verify(badgeService).attribuerBadgeParCodeSiPresent(1L, "NATURE_LOVER");
        verify(badgeService).attribuerBadgeParCodeSiPresent(1L, "POLYVALENT_3_SPORTS");
        verify(badgeService).attribuerBadgeParCodeSiPresent(1L, "TRIATHLETE");
    }
}
