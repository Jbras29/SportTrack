package com.jocf.sporttrack.controller;

import com.jocf.sporttrack.dto.CreerChallengeRequest;
import com.jocf.sporttrack.dto.LigneClassementChallenge;
import com.jocf.sporttrack.model.Challenge;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.service.ChallengeService;
import com.jocf.sporttrack.service.UtilisateurService;
import com.jocf.sporttrack.web.SessionKeys;
import com.jocf.sporttrack.web.SessionUtilisateur;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.ModelAndViewAssert;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ChallengeWebControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ChallengeService challengeService;

    @Mock
    private UtilisateurService utilisateurService;

    @InjectMocks
    private ChallengeWebController challengeWebController;

    @BeforeEach
    void setUp() {
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");

        mockMvc = MockMvcBuilders.standaloneSetup(challengeWebController)
                .setViewResolvers(viewResolver)
                .build();
    }

    private MockHttpSession sessionAvecUtilisateur(long id) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(
                SessionKeys.UTILISATEUR,
                new SessionUtilisateur(id, "user@test.com", "NomTest", "PrenomTest")
        );
        return session;
    }

    private Utilisateur buildUtilisateur(Long id, int hpNormalise) {
        Utilisateur utilisateur = mock(Utilisateur.class);
        lenient().when(utilisateur.getId()).thenReturn(id);
        lenient().when(utilisateur.getHpNormalise()).thenReturn(hpNormalise);
        return utilisateur;
    }

    private Challenge buildChallenge(Long id, Utilisateur organisateur, List<Utilisateur> participants) {
        Challenge challenge = new Challenge();
        challenge.setId(id);
        challenge.setNom("Challenge test");
        challenge.setOrganisateur(organisateur);
        challenge.setParticipants(new java.util.HashSet<>(participants));
        return challenge;
    }


    @Test
    @DisplayName("GET /challenges/creer -> redirige vers login si pas de session")
    void afficherFormulaireCreation_shouldRedirectToLoginWhenNoSessionUser() throws Exception {
        mockMvc.perform(get("/challenges/creer"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("GET /challenges/creer -> affiche le formulaire")
    void afficherFormulaireCreation_shouldReturnFormView() throws Exception {
        MockHttpSession session = sessionAvecUtilisateur(1L);
        Utilisateur utilisateur = buildUtilisateur(1L, 75);

        when(utilisateurService.trouverParId(1L)).thenReturn(Optional.of(utilisateur));

        MvcResult result = mockMvc.perform(get("/challenges/creer").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("challenges/creer-challenge"))
                .andExpect(model().attribute("sessionUserHp", 75))
                .andExpect(model().attribute("navRequestPath", "/challenges"))
                .andReturn();

        ModelAndViewAssert.assertViewName(result.getModelAndView(), "challenges/creer-challenge");
    }

    @Test
    @DisplayName("POST /challenges/creer -> redirige vers login si pas de session")
    void traiterCreationChallenge_shouldRedirectToLoginWhenNoSessionUser() throws Exception {
        mockMvc.perform(post("/challenges/creer")
                        .param("nom", "Mon challenge")
                        .param("dateDebut", "2026-04-01")
                        .param("dateFin", "2026-04-10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("POST /challenges/creer -> crée le challenge et redirige")
    void traiterCreationChallenge_shouldCreateAndRedirect() throws Exception {
        MockHttpSession session = sessionAvecUtilisateur(1L);

        mockMvc.perform(post("/challenges/creer")
                        .session(session)
                        .param("nom", "Mon challenge")
                        .param("dateDebut", "2026-04-01")
                        .param("dateFin", "2026-04-10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/challenges"));

        verify(challengeService).creerChallenge(
                argThat((ArgumentMatcher<CreerChallengeRequest>) req ->
                        req.nom().equals("Mon challenge")
                                && req.dateDebut().equals(LocalDate.of(2026, 4, 1))
                                && req.dateFin().equals(LocalDate.of(2026, 4, 10))),
                eq(1L)
        );
    }

    @Test
    @DisplayName("POST /challenges/creer -> retourne le formulaire avec erreur")
    void traiterCreationChallenge_shouldReturnFormWithError() throws Exception {
        MockHttpSession session = sessionAvecUtilisateur(1L);
        Utilisateur utilisateur = buildUtilisateur(1L, 80);

        when(utilisateurService.trouverParId(1L)).thenReturn(Optional.of(utilisateur));
        doThrow(new IllegalArgumentException("Dates invalides"))
                .when(challengeService).creerChallenge(any(CreerChallengeRequest.class), eq(1L));

        mockMvc.perform(post("/challenges/creer")
                        .session(session)
                        .param("nom", "Mon challenge")
                        .param("dateDebut", "2026-04-10")
                        .param("dateFin", "2026-04-01"))
                .andExpect(status().isOk())
                .andExpect(view().name("challenges/creer-challenge"))
                .andExpect(model().attribute("erreur", "Dates invalides"))
                .andExpect(model().attribute("sessionUserHp", 80))
                .andExpect(model().attribute("navRequestPath", "/challenges"));
    }

    @Test
    @DisplayName("GET /challenges -> affiche la liste")
    void listeDesDefis_shouldReturnListView() throws Exception {
        MockHttpSession session = sessionAvecUtilisateur(1L);
        Challenge challenge = new Challenge();
        challenge.setId(10L);
        challenge.setNom("Challenge A");

        when(challengeService.recupererTousLesChallenges()).thenReturn(List.of(challenge));

        mockMvc.perform(get("/challenges").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("challenges/liste"))
                .andExpect(model().attributeExists("challenges"))
                .andExpect(model().attributeExists("sessionUser"))
                .andExpect(model().attribute("navRequestPath", "/challenges"));
    }

    @Test
    @DisplayName("GET /challenges/{id} -> redirige vers login si pas de session")
    void detailChallenge_shouldRedirectToLoginWhenNoSessionUser() throws Exception {
        mockMvc.perform(get("/challenges/10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("GET /challenges/{id} -> affiche le détail")
    void detailChallenge_shouldReturnDetailView() throws Exception {
        MockHttpSession session = sessionAvecUtilisateur(1L);

        Utilisateur sessionUser = buildUtilisateur(1L, 90);
        Utilisateur autreUtilisateur = buildUtilisateur(2L, 50);
        Utilisateur organisateur = buildUtilisateur(3L, 100);

        Challenge challenge = buildChallenge(10L, organisateur, List.of(sessionUser, autreUtilisateur));

        LigneClassementChallenge ligne1 = new LigneClassementChallenge(sessionUser, 5);
        LigneClassementChallenge ligne2 = new LigneClassementChallenge(autreUtilisateur, 3);

        when(utilisateurService.trouverParId(1L)).thenReturn(Optional.of(sessionUser));
        when(challengeService.trouverParId(10L)).thenReturn(Optional.of(challenge));
        when(challengeService.recupererReponseDuJour(10L, 1L, LocalDate.now())).thenReturn(Boolean.TRUE);
        when(challengeService.getClassement(10L)).thenReturn(List.of(ligne1, ligne2));
        when(utilisateurService.peutAfficherIdentiteVers(sessionUser, 1L)).thenReturn(true);
        when(utilisateurService.peutAfficherIdentiteVers(autreUtilisateur, 1L)).thenReturn(false);
        when(utilisateurService.peutAfficherIdentiteVers(organisateur, 1L)).thenReturn(true);

        MvcResult result = mockMvc.perform(get("/challenges/10").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("challenges/detail"))
                .andExpect(model().attributeExists("challenge"))
                .andExpect(model().attributeExists("classement"))
                .andExpect(model().attribute("estParticipant", true))
                .andExpect(model().attribute("reponseDuJour", true))
                .andExpect(model().attribute("aDejaReponduAujourdhui", true))
                .andExpect(model().attribute("afficherNomOrganisateurChallenge", true))
                .andExpect(model().attribute("sessionUserHp", 90))
                .andExpect(model().attribute("navRequestPath", "/challenges"))
                .andReturn();

        @SuppressWarnings("unchecked")
        Map<Long, Boolean> afficherNomClassement =
                (Map<Long, Boolean>) result.getModelAndView().getModel().get("afficherNomClassement");

        assertEquals(true, afficherNomClassement.get(1L));
        assertEquals(false, afficherNomClassement.get(2L));
    }

    @Test
    @DisplayName("GET /challenges/{id} -> gère un classement avec doublons et un organisateur masqué")
    void detailChallenge_shouldHandleDuplicateRankingAndHiddenOrganizer() throws Exception {
        MockHttpSession session = sessionAvecUtilisateur(1L);
        Utilisateur sessionUser = buildUtilisateur(1L, 90);
        Utilisateur autreUtilisateur = buildUtilisateur(2L, 50);
        Challenge challenge = buildChallenge(10L, null, List.of(sessionUser, autreUtilisateur));

        LigneClassementChallenge ligne1 = new LigneClassementChallenge(sessionUser, 5);
        LigneClassementChallenge ligne2 = new LigneClassementChallenge(sessionUser, 7);

        when(utilisateurService.trouverParId(1L)).thenReturn(Optional.of(sessionUser));
        when(challengeService.trouverParId(10L)).thenReturn(Optional.of(challenge));
        when(challengeService.recupererReponseDuJour(10L, 1L, LocalDate.now())).thenReturn(null);
        when(challengeService.getClassement(10L)).thenReturn(List.of(ligne1, ligne2));
        when(utilisateurService.peutAfficherIdentiteVers(sessionUser, 1L)).thenReturn(true);

        MvcResult result = mockMvc.perform(get("/challenges/10").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("challenges/detail"))
                .andExpect(model().attribute("afficherNomOrganisateurChallenge", false))
                .andExpect(model().attribute("aDejaReponduAujourdhui", false))
                .andReturn();

        @SuppressWarnings("unchecked")
        Map<Long, Boolean> afficherNomClassement =
                (Map<Long, Boolean>) result.getModelAndView().getModel().get("afficherNomClassement");

        assertEquals(true, afficherNomClassement.get(1L));
        assertEquals(1, afficherNomClassement.size());
    }

    @Test
    @DisplayName("GET /challenges/{id} -> lève une erreur si le challenge manque")
    void detailChallenge_shouldThrowWhenChallengeMissing() {
        MockHttpSession session = sessionAvecUtilisateur(1L);
        Utilisateur sessionUser = buildUtilisateur(1L, 90);
        when(utilisateurService.trouverParId(1L)).thenReturn(Optional.of(sessionUser));
        when(challengeService.trouverParId(10L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                challengeWebController.detailChallenge(10L, new org.springframework.ui.ExtendedModelMap(), session));
    }

    @Test
    @DisplayName("POST /challenges/{id}/rejoindre -> redirige vers login si pas de session")
    void rejoindreChallenge_shouldRedirectToLoginWhenNoSessionUser() throws Exception {
        mockMvc.perform(post("/challenges/10/rejoindre"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("POST /challenges/{id}/rejoindre -> bloque si HP à 0")
    void rejoindreChallenge_shouldBlockWhenHpIsZero() throws Exception {
        MockHttpSession session = sessionAvecUtilisateur(1L);
        Utilisateur utilisateur = buildUtilisateur(1L, 0);

        when(utilisateurService.trouverParId(1L)).thenReturn(Optional.of(utilisateur));

        mockMvc.perform(post("/challenges/10/rejoindre").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/challenges/10"))
                .andExpect(flash().attribute("errorHp", "Action bloquée : Votre barre de vie est à 0 !"));

        verify(challengeService, never()).rejoindreChallenge(any(), any());
    }

    @Test
    @DisplayName("POST /challenges/{id}/rejoindre -> rejoint puis redirige")
    void rejoindreChallenge_shouldJoinAndRedirect() throws Exception {
        MockHttpSession session = sessionAvecUtilisateur(1L);
        Utilisateur utilisateur = buildUtilisateur(1L, 10);

        when(utilisateurService.trouverParId(1L)).thenReturn(Optional.of(utilisateur));

        mockMvc.perform(post("/challenges/10/rejoindre").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/challenges/10"));

        verify(challengeService).rejoindreChallenge(10L, 1L);
    }

    @Test
    @DisplayName("POST /challenges/{id}/rejoindre -> ignore l'erreur métier et redirige")
    void rejoindreChallenge_shouldRedirectEvenWhenServiceThrows() throws Exception {
        MockHttpSession session = sessionAvecUtilisateur(1L);
        Utilisateur utilisateur = buildUtilisateur(1L, 10);

        when(utilisateurService.trouverParId(1L)).thenReturn(Optional.of(utilisateur));
        doThrow(new IllegalArgumentException("Challenge terminé"))
                .when(challengeService).rejoindreChallenge(10L, 1L);

        mockMvc.perform(post("/challenges/10/rejoindre").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/challenges/10"));
    }

    @Test
    @DisplayName("POST /challenges/{id}/saisie-quotidienne -> redirige vers login si pas de session")
    void saisieQuotidienne_shouldRedirectToLoginWhenNoSessionUser() throws Exception {
        mockMvc.perform(post("/challenges/10/saisie-quotidienne")
                        .param("realise", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("POST /challenges/{id}/saisie-quotidienne -> enregistre puis redirige")
    void saisieQuotidienne_shouldSaveAndRedirect() throws Exception {
        MockHttpSession session = sessionAvecUtilisateur(1L);

        mockMvc.perform(post("/challenges/10/saisie-quotidienne")
                        .session(session)
                        .param("realise", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/challenges/10"));

        verify(challengeService).enregistrerSaisieQuotidienne(10L, 1L, LocalDate.now(), true);
    }

    @Test
    @DisplayName("POST /challenges/{id}/saisie-quotidienne -> ignore l'erreur et redirige")
    void saisieQuotidienne_shouldRedirectEvenWhenServiceThrows() throws Exception {
        MockHttpSession session = sessionAvecUtilisateur(1L);

        doThrow(new IllegalArgumentException("Pas participant"))
                .when(challengeService).enregistrerSaisieQuotidienne(10L, 1L, LocalDate.now(), true);

        mockMvc.perform(post("/challenges/10/saisie-quotidienne")
                        .session(session)
                        .param("realise", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/challenges/10"));
    }

    @Test
    @DisplayName("POST /challenges/{id}/supprimer -> redirige vers login si pas de session")
    void supprimerChallenge_shouldRedirectToLoginWhenNoSessionUser() throws Exception {
        mockMvc.perform(post("/challenges/10/supprimer"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("POST /challenges/{id}/supprimer -> supprime puis redirige vers la liste")
    void supprimerChallenge_shouldDeleteAndRedirectToList() throws Exception {
        MockHttpSession session = sessionAvecUtilisateur(1L);

        mockMvc.perform(post("/challenges/10/supprimer").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/challenges"));

        verify(challengeService).supprimerChallengeSiOrganisateur(10L, 1L);
    }

    @Test
    @DisplayName("POST /challenges/{id}/supprimer -> redirige vers le détail si erreur")
    void supprimerChallenge_shouldRedirectToDetailWhenServiceThrows() throws Exception {
        MockHttpSession session = sessionAvecUtilisateur(1L);

        doThrow(new IllegalArgumentException("Non autorisé"))
                .when(challengeService).supprimerChallengeSiOrganisateur(10L, 1L);

        mockMvc.perform(post("/challenges/10/supprimer").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/challenges/10"));
    }
}
