package com.jocf.sporttrack.controller;

import com.jocf.sporttrack.model.Activite;
import com.jocf.sporttrack.enumeration.TypeSport;
import com.jocf.sporttrack.enumeration.TypeUtilisateur;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.repository.UtilisateurRepository;
import com.jocf.sporttrack.service.ActiviteService;
import com.jocf.sporttrack.service.UtilisateurService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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
    void supprimerAmi_redirigeLoginSansUtilisateur() {
        when(utilisateurService.trouverParEmail("alice@test.com")).thenReturn(null);

        String view = controller.supprimerAmi(2L, new UsernamePasswordAuthenticationToken("alice@test.com", "x"));

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
