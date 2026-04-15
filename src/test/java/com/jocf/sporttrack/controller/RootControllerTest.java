package com.jocf.sporttrack.controller;

import com.jocf.sporttrack.model.Challenge;
import com.jocf.sporttrack.enumeration.TypeUtilisateur;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.repository.ChallengeRepository;
import com.jocf.sporttrack.service.ActiviteService;
import com.jocf.sporttrack.service.CommentaireService;
import com.jocf.sporttrack.service.UtilisateurService;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RootControllerTest {

    @Mock
    private UtilisateurService utilisateurService;

    @Mock
    private ActiviteService activiteService;

    @Mock
    private CommentaireService commentaireService;

    @Mock
    private ChallengeRepository challengeRepository;

    @InjectMocks
    private RootController controller;

    private Utilisateur utilisateur;

    @BeforeEach
    void setUp() {
        utilisateur = Utilisateur.builder()
                .id(1L)
                .nom("Doe")
                .prenom("Jane")
                .email("jane@test.com")
                .motdepasse("x")
                .typeUtilisateur(TypeUtilisateur.UTILISATEUR)
                .xp(200)
                .hp(90)
                .build();
    }

    @Test
    void racine_redirigeVersLoginQuandAnonyme() {
        var auth = new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));

        String view = controller.racine(auth);

        assertThat(view).isEqualTo("redirect:/login");
    }

    @Test
    void home_redirigeVersLoginQuandNonAuthentifie() {
        String view = controller.home(new ExtendedModelMap(), null);

        assertThat(view).isEqualTo("redirect:/login");
    }

    @Test
    void home_remplitModele() {
        Challenge actif = Challenge.builder()
                .id(1L)
                .dateFin(Date.valueOf(LocalDate.now().plusDays(1)))
                .build();
        when(utilisateurService.trouverParEmailAvecAmis("jane@test.com")).thenReturn(utilisateur);
        when(challengeRepository.findByParticipants_IdOrderByDateFinAsc(1L)).thenReturn(List.of(actif));
        when(activiteService.recupererActivitesFilActualite(utilisateur)).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        String view = controller.home(
                model,
                UsernamePasswordAuthenticationToken.authenticated(
                        "jane@test.com", "x", List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        assertThat(view).isEqualTo("home");
        assertThat(model.getAttribute("user")).isSameAs(utilisateur);
        @SuppressWarnings("unchecked")
        List<Challenge> defis = (List<Challenge>) model.getAttribute("defisEnCours");
        assertThat(defis).containsExactly(actif);
    }

    @Test
    void homeAdmin_ajouteCollectionsAdmin() {
        when(utilisateurService.trouverParEmailAvecAmis("jane@test.com")).thenReturn(utilisateur);
        when(challengeRepository.findByParticipants_IdOrderByDateFinAsc(1L)).thenReturn(List.of());
        when(activiteService.recupererActivitesFilActualite(utilisateur)).thenReturn(List.of());
        when(utilisateurService.recupererTousLesUtilisateurs()).thenReturn(List.of(utilisateur));
        when(commentaireService.recupererTousLesCommentaires()).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        String view = controller.homeAdmin(
                model,
                UsernamePasswordAuthenticationToken.authenticated(
                        "jane@test.com", "x", List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        assertThat(view).isEqualTo("homeAdmin");
        assertThat(model.getAttribute("tousUtilisateurs")).isEqualTo(List.of(utilisateur));
    }

    @Test
    void creerEvenementPage_retourneVue() {
        when(utilisateurService.trouverParEmail("jane@test.com")).thenReturn(utilisateur);
        Model model = new ExtendedModelMap();

        String view = controller.creerEvenementPage(
                UsernamePasswordAuthenticationToken.authenticated(
                        "jane@test.com", "x", List.of(new SimpleGrantedAuthority("ROLE_USER"))),
                model);

        assertThat(view).isEqualTo("evenement/creer-evenement");
        assertThat(model.getAttribute("user")).isSameAs(utilisateur);
    }
}
