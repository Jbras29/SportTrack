package com.jocf.sporttrack.controller;

import com.jocf.sporttrack.dto.CreerChallengeRequest;
import com.jocf.sporttrack.dto.LigneClassementChallenge;
import com.jocf.sporttrack.model.Challenge;
import com.jocf.sporttrack.enumeration.TypeUtilisateur;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.service.ChallengeService;
import com.jocf.sporttrack.service.UtilisateurService;
import com.jocf.sporttrack.web.SessionKeys;
import com.jocf.sporttrack.web.SessionUtilisateur;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChallengeWebControllerTest {

    @Mock
    private ChallengeService challengeService;

    @Mock
    private UtilisateurService utilisateurService;

    @InjectMocks
    private ChallengeWebController controller;

    private MockHttpSession session;
    private SessionUtilisateur sessionUtilisateur;
    private Utilisateur utilisateur;

    @BeforeEach
    void setUp() {
        sessionUtilisateur = new SessionUtilisateur(1L, "jane@test.com", "Doe", "Jane");
        session = new MockHttpSession();
        session.setAttribute(SessionKeys.UTILISATEUR, sessionUtilisateur);
        utilisateur = Utilisateur.builder()
                .id(1L)
                .nom("Doe")
                .prenom("Jane")
                .email("jane@test.com")
                .motdepasse("x")
                .typeUtilisateur(TypeUtilisateur.UTILISATEUR)
                .hp(80)
                .build();
    }

    @Test
    void afficherFormulaireCreation_redirigeLoginSansSession() {
        String view = controller.afficherFormulaireCreation(new ExtendedModelMap(), new MockHttpSession());

        assertThat(view).isEqualTo("redirect:/login");
    }

    @Test
    void afficherFormulaireCreation_remplitModele() {
        Model model = new ExtendedModelMap();
        when(utilisateurService.trouverParId(1L)).thenReturn(Optional.of(utilisateur));

        String view = controller.afficherFormulaireCreation(model, session);

        assertThat(view).isEqualTo("challenges/creer-challenge");
        assertThat(model.getAttribute("sessionUserHp")).isEqualTo(utilisateur.getHpNormalise());
    }

    @Test
    void traiterCreationChallenge_redirigeVersListeQuandSucces() {
        String view = controller.traiterCreationChallenge(
                "Défi",
                Date.valueOf(LocalDate.of(2026, 4, 1)),
                Date.valueOf(LocalDate.of(2026, 4, 10)),
                session,
                new ExtendedModelMap());

        assertThat(view).isEqualTo("redirect:/challenges");
        ArgumentCaptor<CreerChallengeRequest> captor = ArgumentCaptor.forClass(CreerChallengeRequest.class);
        verify(challengeService).creerChallenge(captor.capture(), org.mockito.ArgumentMatchers.eq(1L));
        assertThat(captor.getValue().nom()).isEqualTo("Défi");
    }

    @Test
    void listeDesDefis_remplitModele() {
        Challenge challenge = Challenge.builder().id(2L).nom("C").build();
        when(challengeService.recupererTousLesChallenges()).thenReturn(List.of(challenge));
        Model model = new ExtendedModelMap();

        String view = controller.listeDesDefis(model, session);

        assertThat(view).isEqualTo("challenges/liste");
        assertThat(model.getAttribute("sessionUser")).isEqualTo(sessionUtilisateur);
    }

    @Test
    void detailChallenge_redirigeLoginSansSession() {
        String view = controller.detailChallenge(2L, new ExtendedModelMap(), new MockHttpSession());

        assertThat(view).isEqualTo("redirect:/login");
    }

    @Test
    void detailChallenge_remplitModele() {
        Challenge challenge = Challenge.builder()
                .id(2L)
                .nom("Défi")
                .organisateur(utilisateur)
                .participants(Set.of(utilisateur))
                .build();
        when(utilisateurService.trouverParId(1L)).thenReturn(Optional.of(utilisateur));
        when(challengeService.trouverParId(2L)).thenReturn(Optional.of(challenge));
        when(challengeService.recupererReponseDuJour(2L, 1L, LocalDate.now())).thenReturn(Boolean.TRUE);
        when(challengeService.getClassement(2L)).thenReturn(List.of(new LigneClassementChallenge(utilisateur, 5L)));
        when(utilisateurService.peutAfficherIdentiteVers(utilisateur, 1L)).thenReturn(true);

        Model model = new ExtendedModelMap();
        String view = controller.detailChallenge(2L, model, session);

        assertThat(view).isEqualTo("challenges/detail");
        assertThat(model.getAttribute("challenge")).isSameAs(challenge);
        assertThat(model.getAttribute("aDejaReponduAujourdhui")).isEqualTo(true);
    }

    @Test
    void rejoindreChallenge_bloqueQuandHpZero() {
        utilisateur.setHp(0);
        when(utilisateurService.trouverParId(1L)).thenReturn(Optional.of(utilisateur));
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.rejoindreChallenge(4L, session, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/challenges/4");
        assertThat(redirectAttributes.getFlashAttributes().get("errorHp"))
                .isEqualTo("Action bloquée : Votre barre de vie est à 0 !");
    }

    @Test
    void saisieQuotidienne_redirigeVersDetail() {
        String view = controller.saisieQuotidienne(4L, true, session);

        assertThat(view).isEqualTo("redirect:/challenges/4");
        verify(challengeService).enregistrerSaisieQuotidienne(4L, 1L, LocalDate.now(), true);
    }

    @Test
    void supprimerChallenge_redirigeVersListeQuandSucces() {
        String view = controller.supprimerChallenge(4L, session);

        assertThat(view).isEqualTo("redirect:/challenges");
        verify(challengeService).supprimerChallengeSiOrganisateur(4L, 1L);
    }
}
