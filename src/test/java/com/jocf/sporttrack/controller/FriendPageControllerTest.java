package com.jocf.sporttrack.controller;

import com.jocf.sporttrack.model.Activite;
import com.jocf.sporttrack.enumeration.TypeSport;
import com.jocf.sporttrack.enumeration.TypeUtilisateur;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.repository.UtilisateurRepository;
import com.jocf.sporttrack.service.ActiviteService;
import com.jocf.sporttrack.service.UtilisateurService;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FriendPageControllerTest {

    @Mock
    private UtilisateurService utilisateurService;

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private ActiviteService activiteService;

    @InjectMocks
    private FriendPageController controller;

    private Utilisateur courant;
    private Utilisateur ami;

    @BeforeEach
    void setUp() {
        ami = utilisateur(2L, "Bob", "Smith");
        courant = utilisateur(1L, "Alice", "Doe");
        courant.setAmis(new ArrayList<>(List.of(ami)));
    }

    @Test
    void afficherPageAmis_redirigeVersLoginSansAuth() {
        String view = controller.afficherPageAmis(null, null, new ExtendedModelMap());

        assertThat(view).isEqualTo("redirect:/login");
    }

    @Test
    void afficherPageAmis_remplitModele() {
        Model model = new ExtendedModelMap();
        Activite activite = Activite.builder()
                .id(4L)
                .nom("Run")
                .typeSport(TypeSport.COURSE)
                .date(LocalDate.now())
                .distance(10.0)
                .utilisateur(ami)
                .build();
        when(utilisateurService.trouverParEmailAvecAmis("alice@test.com")).thenReturn(courant);
        when(activiteService.recupererActivitesDesAmis(courant)).thenReturn(List.of(activite));
        when(utilisateurRepository.findDemandesAmisRecuesByUtilisateurId(1L)).thenReturn(List.of());

        String view = controller.afficherPageAmis(
                null,
                new UsernamePasswordAuthenticationToken("alice@test.com", "x"),
                model);

        assertThat(view).isEqualTo("friend/index");
        assertThat(model.getAttribute("resumePage")).isEqualTo("Vous avez 1 ami(s) dans votre reseau Sport Track.");
        assertThat((List<?>) model.getAttribute("amis")).hasSize(1);
    }

    @Test
    void afficherPageAmis_filtreTrieEtExposeLesDetailsDesActivites() {
        Utilisateur autreAmi = utilisateur(3L, "Charlie", "Brown");
        courant.setAmis(new ArrayList<>(List.of(ami, autreAmi)));

        Activite activiteRecente = Activite.builder()
                .id(4L)
                .nom("")
                .typeSport(TypeSport.CYCLISME)
                .date(LocalDate.now().minusDays(1))
                .distance(10.5)
                .utilisateur(ami)
                .build();
        Activite activiteAncienne = Activite.builder()
                .id(5L)
                .typeSport(TypeSport.COURSE)
                .date(LocalDate.now().minusDays(10))
                .distance(5.0)
                .utilisateur(autreAmi)
                .build();

        when(utilisateurService.trouverParEmailAvecAmis("alice@test.com")).thenReturn(courant);
        when(activiteService.recupererActivitesDesAmis(courant)).thenReturn(List.of(activiteAncienne, activiteRecente));
        when(utilisateurRepository.findDemandesAmisRecuesByUtilisateurId(1L))
                .thenReturn(List.of(utilisateur(6L, "Zed", "Alpha"), utilisateur(7L, "Amy", "Beta")));

        Model model = new ExtendedModelMap();
        String view = controller.afficherPageAmis(
                "bob",
                new UsernamePasswordAuthenticationToken("alice@test.com", "x"),
                model);

        assertThat(view).isEqualTo("friend/index");
        assertThat(model.getAttribute("resumePage")).isEqualTo("1 ami(s) correspondent a votre recherche.");
        assertThat(model.getAttribute("demandesRecuesCount")).isEqualTo(2);

        @SuppressWarnings("unchecked")
        List<FriendPageController.FriendCardView> amis =
                (List<FriendPageController.FriendCardView>) model.getAttribute("amis");
        assertThat(amis).hasSize(1);
        FriendPageController.FriendCardView carte = amis.get(0);
        assertThat(carte.id()).isEqualTo(2L);
        assertThat(carte.styleBanniere())
                .isEqualTo("background: linear-gradient(135deg, var(--brand-green), #a3cc52);");
        assertThat(carte.aDerniereActivite()).isTrue();
        assertThat(carte.actifRecemment()).isTrue();
        assertThat(carte.derniereActiviteTitre()).isEqualTo("Cyclisme");
        assertThat(carte.derniereActiviteDetails()).isEqualTo("Cyclisme • Hier • 10,5 km");

        @SuppressWarnings("unchecked")
        List<FriendPageController.RequestCardView> demandes =
                (List<FriendPageController.RequestCardView>) model.getAttribute("demandesRecues");
        assertThat(demandes).extracting(FriendPageController.RequestCardView::nomComplet)
                .containsExactly("Amy Beta", "Zed Alpha");
    }

    @Test
    void afficherPageAmis_retourneUnResumeSansResultatQuandAucunAmiNeCorrespond() {
        courant.setAmis(new ArrayList<>(List.of(ami)));
        when(utilisateurService.trouverParEmailAvecAmis("alice@test.com")).thenReturn(courant);
        when(activiteService.recupererActivitesDesAmis(courant)).thenReturn(List.of());
        when(utilisateurRepository.findDemandesAmisRecuesByUtilisateurId(1L)).thenReturn(List.of());

        Model model = new ExtendedModelMap();
        String view = controller.afficherPageAmis(
                "zzz",
                new UsernamePasswordAuthenticationToken("alice@test.com", "x"),
                model);

        assertThat(view).isEqualTo("friend/index");
        assertThat(model.getAttribute("resumePage")).isEqualTo("Aucun ami ne correspond a votre recherche.");
        assertThat((List<?>) model.getAttribute("amis")).isEmpty();
    }

    @Test
    void afficherPageAmis_redirigeQuandLeNomEstVide() {
        String view = controller.afficherPageAmis(
                null,
                UsernamePasswordAuthenticationToken.authenticated("", "x", List.of()),
                new ExtendedModelMap());

        assertThat(view).isEqualTo("redirect:/login");
    }

    @Test
    void afficherPageAmis_redirigeQuandLeNomEstComposeSeulementDespces() {
        String view = controller.afficherPageAmis(
                null,
                UsernamePasswordAuthenticationToken.authenticated("   ", "x", List.of()),
                new ExtendedModelMap());

        assertThat(view).isEqualTo("redirect:/login");
    }

    @Test
    void afficherPageAmis_afficheUneDateBruteEtLeStyleModuloZero() {
        Utilisateur utilisateurModuloZero = utilisateur(4L, "Dana", "Zero");
        courant.setAmis(new ArrayList<>(List.of(utilisateurModuloZero)));

        Activite activiteAncienne = Activite.builder()
                .id(11L)
                .nom("Sortie longue")
                .typeSport(TypeSport.COURSE)
                .date(LocalDate.now().minusDays(10))
                .distance(12.0)
                .utilisateur(utilisateurModuloZero)
                .build();
        when(utilisateurService.trouverParEmailAvecAmis("alice@test.com")).thenReturn(courant);
        when(activiteService.recupererActivitesDesAmis(courant)).thenReturn(List.of(activiteAncienne));
        when(utilisateurRepository.findDemandesAmisRecuesByUtilisateurId(1L)).thenReturn(List.of());

        Model model = new ExtendedModelMap();
        String view = controller.afficherPageAmis(
                null,
                new UsernamePasswordAuthenticationToken("alice@test.com", "x"),
                model);

        assertThat(view).isEqualTo("friend/index");
        @SuppressWarnings("unchecked")
        List<FriendPageController.FriendCardView> amis =
                (List<FriendPageController.FriendCardView>) model.getAttribute("amis");
        assertThat(amis).hasSize(1);
        assertThat(amis.get(0).styleBanniere())
                .isEqualTo("background: linear-gradient(135deg, var(--brand-blue), var(--brand-light-blue));");
        assertThat(amis.get(0).derniereActiviteDetails())
                .contains(LocalDate.now().minusDays(10).toString())
                .contains("12 km");
    }

    @Test
    void afficherPageAmis_ignoreLesActivitesSansUtilisateurValide_etTrieParDateEtNom() {
        Utilisateur autreAmi = utilisateur(3L, "Charlie", "Brown");
        courant.setAmis(new ArrayList<>(List.of(ami, autreAmi)));

        Activite activiteSansUtilisateur = Activite.builder()
                .id(9L)
                .date(LocalDate.now())
                .build();
        Activite activiteSansId = Activite.builder()
                .id(8L)
                .date(LocalDate.now())
                .utilisateur(Utilisateur.builder().prenom("Ghost").nom("NoId").build())
                .build();
        Activite activiteLongue = Activite.builder()
                .id(7L)
                .nom(null)
                .typeSport(TypeSport.COURSE_A_PIED)
                .date(LocalDate.now().minusDays(5))
                .distance(10.0)
                .utilisateur(autreAmi)
                .build();

        when(utilisateurService.trouverParEmailAvecAmis("alice@test.com")).thenReturn(courant);
        when(activiteService.recupererActivitesDesAmis(courant)).thenReturn(List.of(activiteSansUtilisateur, activiteSansId, activiteLongue));
        when(utilisateurRepository.findDemandesAmisRecuesByUtilisateurId(1L)).thenReturn(List.of());

        Model model = new ExtendedModelMap();
        String view = controller.afficherPageAmis(null, new UsernamePasswordAuthenticationToken("alice@test.com", "x"), model);

        assertThat(view).isEqualTo("friend/index");
        @SuppressWarnings("unchecked")
        List<FriendPageController.FriendCardView> amis =
                (List<FriendPageController.FriendCardView>) model.getAttribute("amis");
        assertThat(amis).hasSize(2);
        assertThat(amis.get(0).nomComplet()).isEqualTo("Charlie Brown");
        assertThat(amis.get(0).derniereActiviteTitre()).isEqualTo("Course a pied");
        assertThat(amis.get(0).derniereActiviteDetails()).contains("Il y a 5 jours").contains("10 km");
        assertThat(amis.get(1).derniereActiviteTitre()).isEqualTo("Aucune activite recente");
    }

    @Test
    void afficherPageAmis_gereUneRechercheComposeeSeulementDespces() {
        courant.setAmis(new ArrayList<>(List.of(ami)));
        when(utilisateurService.trouverParEmailAvecAmis("alice@test.com")).thenReturn(courant);
        when(activiteService.recupererActivitesDesAmis(courant)).thenReturn(List.of());
        when(utilisateurRepository.findDemandesAmisRecuesByUtilisateurId(1L)).thenReturn(List.of());

        Model model = new ExtendedModelMap();
        String view = controller.afficherPageAmis(
                "   ",
                new UsernamePasswordAuthenticationToken("alice@test.com", "x"),
                model);

        assertThat(view).isEqualTo("friend/index");
        assertThat(model.getAttribute("resumePage")).isEqualTo("Vous avez 1 ami(s) dans votre reseau Sport Track.");
        assertThat((List<?>) model.getAttribute("amis")).hasSize(1);
    }

    @Test
    void constructionDesDetailsEtDuStyle_couvrentLesBranchesRestantes() throws Exception {
        Method titre = FriendPageController.class.getDeclaredMethod("construireTitreActivite", Activite.class);
        Method details = FriendPageController.class.getDeclaredMethod("construireDetailsActivite", Activite.class);
        Method style = FriendPageController.class.getDeclaredMethod("construireStyleBanniere", Long.class);
        Method recherche = FriendPageController.class.getDeclaredMethod("correspondRecherche", Utilisateur.class, String.class);
        titre.setAccessible(true);
        details.setAccessible(true);
        style.setAccessible(true);
        recherche.setAccessible(true);

        Activite sansNomNiType = Activite.builder().date(LocalDate.now()).build();
        Activite avecType = Activite.builder().typeSport(TypeSport.CYCLISME).date(LocalDate.now()).build();
        Activite avecNom = Activite.builder().nom("Sortie").build();
        Activite detailsTypeSeulement = Activite.builder().typeSport(TypeSport.COURSE).build();
        Activite detailsRien = Activite.builder().build();

        assertThat((String) titre.invoke(controller, new Object[] {sansNomNiType})).isEqualTo("Activite sportive");
        assertThat((String) titre.invoke(controller, new Object[] {avecType})).isEqualTo("Cyclisme");
        assertThat((String) titre.invoke(controller, new Object[] {avecNom})).isEqualTo("Sortie");
        assertThat((String) details.invoke(controller, new Object[] {detailsTypeSeulement})).isEqualTo("Course");
        assertThat((String) details.invoke(controller, new Object[] {detailsRien})).isEqualTo("Activite recente");
        assertThat((String) style.invoke(controller, new Object[] {null})).contains("brand-blue");
        assertThat((String) style.invoke(controller, new Object[] {1L})).contains("brand-orange");
        assertThat((String) style.invoke(controller, new Object[] {2L})).contains("brand-green");
        assertThat((String) style.invoke(controller, new Object[] {3L})).contains("#4338ca");
        assertThat((Boolean) recherche.invoke(controller, ami, "   ")).isTrue();
        assertThat((Boolean) recherche.invoke(controller, ami, "bob")).isTrue();
        assertThat((Boolean) recherche.invoke(controller, ami, "zzz")).isFalse();
    }

    @Test
    void supprimerAmi_redirigeLoginSansUtilisateur() {
        when(utilisateurService.trouverParEmail("alice@test.com")).thenReturn(null);

        String view = controller.supprimerAmi(2L, new UsernamePasswordAuthenticationToken("alice@test.com", "x"));

        assertThat(view).isEqualTo("redirect:/login");
    }

    @Test
    void supprimerAmi_redirigeLoginSansAuthentication() {
        String view = controller.supprimerAmi(2L, null);

        assertThat(view).isEqualTo("redirect:/login");
    }

    @Test
    void supprimerAmi_appelleService() {
        when(utilisateurService.trouverParEmail("alice@test.com")).thenReturn(courant);

        String view = controller.supprimerAmi(2L, new UsernamePasswordAuthenticationToken("alice@test.com", "x"));

        assertThat(view).isEqualTo("redirect:/friend");
        verify(utilisateurService).supprimerAmi(1L, 2L);
    }

    private static Utilisateur utilisateur(Long id, String prenom, String nom) {
        return Utilisateur.builder()
                .id(id)
                .prenom(prenom)
                .nom(nom)
                .email(prenom.toLowerCase() + "@test.com")
                .motdepasse("x")
                .typeUtilisateur(TypeUtilisateur.UTILISATEUR)
                .amis(new ArrayList<>())
                .build();
    }
}
