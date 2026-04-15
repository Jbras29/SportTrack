package com.jocf.sporttrack.service;

import com.jocf.sporttrack.model.Activite;
import com.jocf.sporttrack.enumeration.TypeSport;
import com.jocf.sporttrack.enumeration.TypeUtilisateur;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.repository.ActiviteRepository;
import java.time.LocalDate;
import java.util.ArrayList;
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
    void evaluerEtAttribuerBadges_declenchePremierPasEtPremierChrono() {
        Utilisateur user = Utilisateur.builder()
                .id(1L)
                .nom("Doe")
                .prenom("Jane")
                .email("jane@test.com")
                .motdepasse("x")
                .typeUtilisateur(TypeUtilisateur.UTILISATEUR)
                .build();
        Activite activite = activite(10L, user, TypeSport.COURSE, 5000.0, 65, LocalDate.now(), "Parc", "Run");
        when(activiteRepository.findByUtilisateur(user)).thenReturn(List.of(activite));

        service.evaluerEtAttribuerBadges(activite);

        verify(badgeService).attribuerBadgeParCodeSiPresent(1L, "PREMIER_PAS");
        verify(badgeService).attribuerBadgeParCodeSiPresent(1L, "PREMIER_CHRONO");
        verify(badgeService).attribuerBadgeParCodeSiPresent(1L, "CINQ_K_STARTER");
        verify(badgeService).attribuerBadgeParCodeSiPresent(1L, "ENDURANCE_1H");
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
        List<Activite> activites = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            TypeSport typeSport = switch (i % 3) {
                case 0 -> TypeSport.COURSE;
                case 1 -> TypeSport.NATATION;
                default -> TypeSport.CYCLISME;
            };
            String location = switch (i % 3) {
                case 0 -> "Parc naturel";
                case 1 -> "Ville B";
                default -> "Ville C";
            };
            activites.add(activite(
                    (long) i + 1L,
                    user,
                    typeSport,
                    i == 0 ? 42000.0 : 1000.0,
                    i == 0 ? 130 : 30,
                    LocalDate.now().minusDays(i),
                    location,
                    "Activite " + i));
        }
        Activite activite = activites.get(0);
        when(activiteRepository.findByUtilisateur(user)).thenReturn(activites);

        service.evaluerEtAttribuerBadges(activite);

        verify(badgeService).attribuerBadgeParCodeSiPresent(1L, "RECORD_PERSONNEL");
        verify(badgeService).attribuerBadgeParCodeSiPresent(1L, "SERIE_7_JOURS");
        verify(badgeService).attribuerBadgeParCodeSiPresent(1L, "STREAK_30_JOURS");
        verify(badgeService).attribuerBadgeParCodeSiPresent(1L, "CENT_ACTIVITES");
        verify(badgeService).attribuerBadgeParCodeSiPresent(1L, "EXPLORATEUR_3_LIEUX");
        verify(badgeService).attribuerBadgeParCodeSiPresent(1L, "GLOBE_TROTTER");
        verify(badgeService).attribuerBadgeParCodeSiPresent(1L, "NATURE_LOVER");
        verify(badgeService).attribuerBadgeParCodeSiPresent(1L, "POLYVALENT_3_SPORTS");
        verify(badgeService).attribuerBadgeParCodeSiPresent(1L, "TRIATHLETE");
        verify(badgeService).attribuerBadgeParCodeSiPresent(1L, "IRON_WILL_2H");
    }

    private static Activite activite(
            Long id,
            Utilisateur user,
            TypeSport typeSport,
            double distance,
            int temps,
            LocalDate date,
            String location,
            String nom) {
        return Activite.builder()
                .id(id)
                .utilisateur(user)
                .typeSport(typeSport)
                .distance(distance)
                .temps(temps)
                .date(date)
                .location(location)
                .nom(nom)
                .build();
    }
}
