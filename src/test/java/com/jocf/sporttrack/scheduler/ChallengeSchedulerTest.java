package com.jocf.sporttrack.scheduler;

import com.jocf.sporttrack.dto.LigneClassementChallenge;
import com.jocf.sporttrack.model.Challenge;
import com.jocf.sporttrack.enumeration.TypeUtilisateur;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.service.ChallengeService;
import com.jocf.sporttrack.service.UtilisateurService;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChallengeSchedulerTest {

    @Mock
    private ChallengeService challengeService;

    @Mock
    private UtilisateurService utilisateurService;

    @InjectMocks
    private ChallengeScheduler scheduler;

    @Test
    void verifierLesChallengesTermines_appliqueUnePunitionAuxPerdants() {
        Utilisateur user = Utilisateur.builder()
                .id(1L).nom("Doe").prenom("Jane").email("jane@test.com").motdepasse("x")
                .typeUtilisateur(TypeUtilisateur.UTILISATEUR).build();
        Challenge challenge = Challenge.builder()
                .id(2L)
                .dateDebut(Date.valueOf(LocalDate.now().minusDays(2)))
                .dateFin(Date.valueOf(LocalDate.now().minusDays(1)))
                .build();
        when(challengeService.trouverChallengesTerminesLe(LocalDate.now().minusDays(1))).thenReturn(List.of(challenge));
        when(challengeService.getClassement(2L)).thenReturn(List.of(new LigneClassementChallenge(user, 0L)));

        scheduler.verifierLesChallengesTermines();

        verify(utilisateurService).appliquerPunitionChallenge(1L, 20);
    }

    @Test
    void verifierLesChallengesTermines_ignoreLesParticipantsAvecUnScoreSuffisant() {
        Utilisateur user = Utilisateur.builder()
                .id(1L).nom("Doe").prenom("Jane").email("jane@test.com").motdepasse("x")
                .typeUtilisateur(TypeUtilisateur.UTILISATEUR).build();
        Challenge challenge = Challenge.builder()
                .id(2L)
                .dateDebut(Date.valueOf(LocalDate.now().minusDays(2)))
                .dateFin(Date.valueOf(LocalDate.now().minusDays(1)))
                .build();
        when(challengeService.trouverChallengesTerminesLe(LocalDate.now().minusDays(1))).thenReturn(List.of(challenge));
        when(challengeService.getClassement(2L)).thenReturn(List.of(new LigneClassementChallenge(user, 2L)));

        scheduler.verifierLesChallengesTermines();

        verify(utilisateurService, org.mockito.Mockito.never()).appliquerPunitionChallenge(1L, 20);
    }

    @Test
    void sanctionnerLesAbsencesQuotidiennes_delegueAuService() {
        scheduler.sanctionnerLesAbsencesQuotidiennes();

        verify(challengeService).sanctionnerAbsencesQuotidiennes(LocalDate.now().minusDays(1));
    }
}
