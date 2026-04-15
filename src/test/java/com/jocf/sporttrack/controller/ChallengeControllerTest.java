package com.jocf.sporttrack.controller;

import com.jocf.sporttrack.dto.CreerChallengeRequest;
import com.jocf.sporttrack.dto.ModifierChallengeRequest;
import com.jocf.sporttrack.model.Challenge;
import com.jocf.sporttrack.enumeration.TypeUtilisateur;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.service.ChallengeService;
import com.jocf.sporttrack.service.UtilisateurService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChallengeControllerTest {

    @Mock
    private ChallengeService challengeService;

    @Mock
    private UtilisateurService utilisateurService;

    @InjectMocks
    private ChallengeController controller;

    @Test
    void getAllChallenges_retourneListe() {
        Challenge challenge = Challenge.builder().id(1L).nom("C").build();
        when(challengeService.recupererTousLesChallenges()).thenReturn(List.of(challenge));

        var response = controller.getAllChallenges();

        assertThat(response.getBody()).containsExactly(challenge);
    }

    @Test
    void getChallengeById_retourne404QuandAbsent() {
        when(challengeService.trouverParId(2L)).thenReturn(Optional.empty());

        var response = controller.getChallengeById(2L);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void createChallenge_retourne404QuandOrganisateurAbsent() {
        when(utilisateurService.trouverParId(1L)).thenReturn(Optional.empty());

        var response = controller.createChallenge(new CreerChallengeRequest("C", LocalDate.now(), null), 1L);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isEqualTo("Utilisateur non trouvé.");
    }

    @Test
    void createChallenge_retourne403QuandHpAZero() {
        Utilisateur user = Utilisateur.builder()
                .id(1L)
                .nom("Admin")
                .prenom("A")
                .email("a@test.com")
                .motdepasse("x")
                .typeUtilisateur(TypeUtilisateur.UTILISATEUR)
                .hp(0)
                .build();
        when(utilisateurService.trouverParId(1L)).thenReturn(Optional.of(user));

        var response = controller.createChallenge(new CreerChallengeRequest("C", LocalDate.now(), null), 1L);

        assertThat(response.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void createChallenge_retourneChallengeQuandSucces() {
        Utilisateur user = Utilisateur.builder()
                .id(1L)
                .nom("Admin")
                .prenom("A")
                .email("a@test.com")
                .motdepasse("x")
                .typeUtilisateur(TypeUtilisateur.UTILISATEUR)
                .hp(80)
                .build();
        Challenge created = Challenge.builder().id(7L).nom("C").build();
        when(utilisateurService.trouverParId(1L)).thenReturn(Optional.of(user));
        when(challengeService.creerChallenge(any(CreerChallengeRequest.class), org.mockito.ArgumentMatchers.eq(1L)))
                .thenReturn(created);

        var response = controller.createChallenge(new CreerChallengeRequest("C", LocalDate.now(), null), 1L);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(created);
    }

    @Test
    void updateChallenge_retourne404QuandErreur() {
        when(challengeService.modifierChallenge(org.mockito.ArgumentMatchers.eq(3L), any(ModifierChallengeRequest.class)))
                .thenThrow(new IllegalArgumentException("introuvable"));

        var response = controller.updateChallenge(3L, new ModifierChallengeRequest("X", LocalDate.now(), null));

        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void deleteChallenge_retourne204QuandSucces() {
        var response = controller.deleteChallenge(4L);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
    }
}
