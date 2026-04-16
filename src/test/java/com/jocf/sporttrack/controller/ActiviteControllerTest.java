package com.jocf.sporttrack.controller;

import com.jocf.sporttrack.dto.CreerActiviteCommand;
import com.jocf.sporttrack.dto.ModifierActiviteRequest;
import com.jocf.sporttrack.model.Activite;
import com.jocf.sporttrack.enumeration.TypeSport;
import com.jocf.sporttrack.enumeration.TypeUtilisateur;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.service.ActiviteService;
import com.jocf.sporttrack.service.OpenMeteoService;
import com.jocf.sporttrack.service.UtilisateurService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActiviteControllerTest {

    @Mock
    private ActiviteService activiteService;

    @Mock
    private UtilisateurService utilisateurService;

    @Mock
    private OpenMeteoService openMeteoService;

    @InjectMocks
    private ActiviteController controller;

    private Utilisateur utilisateur;
    private Activite activite;

    @BeforeEach
    void setUp() {
        utilisateur = Utilisateur.builder()
                .id(1L)
                .nom("Doe")
                .prenom("Jane")
                .email("jane@test.com")
                .motdepasse("secret")
                .typeUtilisateur(TypeUtilisateur.UTILISATEUR)
                .amis(List.of())
                .build();

        activite = Activite.builder()
                .id(9L)
                .nom("Sortie")
                .typeSport(TypeSport.COURSE)
                .date(LocalDate.of(2026, 4, 1))
                .utilisateur(utilisateur)
                .build();
    }

    @Test
    void getAllActivites_retourne200() {
        when(activiteService.recupererToutesLesActivites()).thenReturn(List.of(activite));

        var response = controller.getAllActivites();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsExactly(activite);
    }

    @Test
    void getActiviteById_retourneActiviteQuandTrouvee() {
        when(activiteService.trouverParId(9L)).thenReturn(Optional.of(activite));

        var response = controller.getActiviteById(9L);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(activite);
    }

    @Test
    void getActiviteById_retourne404QuandAbsente() {
        when(activiteService.trouverParId(9L)).thenReturn(Optional.empty());

        var response = controller.getActiviteById(9L);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void detailActivite_ajouteActiviteEtUtilisateur() {
        Model model = new ExtendedModelMap();
        var auth = new UsernamePasswordAuthenticationToken(
                "jane@test.com", "x", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        when(activiteService.trouverParId(9L)).thenReturn(Optional.of(activite));
        when(utilisateurService.trouverParEmail("jane@test.com")).thenReturn(utilisateur);

        String view = controller.detailActivite(9L, model, auth);

        assertThat(view).isEqualTo("activity/detail");
        assertThat(model.getAttribute("activite")).isSameAs(activite);
        assertThat(model.getAttribute("user")).isSameAs(utilisateur);
    }

    @Test
    void detailActivite_redirigeVersProfileQuandAbsente() {
        when(activiteService.trouverParId(9L)).thenReturn(Optional.empty());

        String view = controller.detailActivite(9L, new ExtendedModelMap(), null);

        assertThat(view).isEqualTo("redirect:/profile");
    }

    @Test
    void getActivitesByUtilisateur_retourne404QuandServiceEchoue() {
        when(activiteService.recupererActivitesParUtilisateur(7L))
                .thenThrow(new IllegalArgumentException("Utilisateur introuvable"));

        var response = controller.getActivitesByUtilisateur(7L);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void getActivitesByUtilisateur_retourneListeQuandTrouvee() {
        when(activiteService.recupererActivitesParUtilisateur(1L)).thenReturn(List.of(activite));

        var response = controller.getActivitesByUtilisateur(1L);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsExactly(activite);
    }

    @Test
    void getActivitesByTypeSport_retourneListe() {
        when(activiteService.recupererActivitesParTypeSport(TypeSport.COURSE)).thenReturn(List.of(activite));

        var response = controller.getActivitesByTypeSport(TypeSport.COURSE);

        assertThat(response.getBody()).containsExactly(activite);
    }

    @Test
    void locationSuggestions_retourneLesSuggestionsDuService() {
        when(activiteService.recupererSuggestionsLocations("Pa", 8)).thenReturn(List.of("Paris", "Pau"));

        var response = controller.locationSuggestions("Pa");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsExactly("Paris", "Pau");
    }

    @Test
    void createActivite_vueAjouteTypesEtAmis() {
        Model model = new ExtendedModelMap();
        var auth = new UsernamePasswordAuthenticationToken(
                "jane@test.com", "x", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        Utilisateur ami = Utilisateur.builder()
                .id(2L)
                .nom("Friend")
                .prenom("Joe")
                .email("joe@test.com")
                .motdepasse("x")
                .typeUtilisateur(TypeUtilisateur.UTILISATEUR)
                .build();
        utilisateur.setAmis(List.of(ami));
        when(utilisateurService.trouverParEmail("jane@test.com")).thenReturn(utilisateur);

        String view = controller.createActivite(model, auth);

        assertThat(view).isEqualTo("activity/create");
        assertThat((Object[]) model.getAttribute("typesSportifs")).contains(TypeSport.COURSE);
        @SuppressWarnings("unchecked")
        List<Utilisateur> amis = (List<Utilisateur>) model.getAttribute("amis");
        assertThat(amis).containsExactly(ami);
    }

    @Test
    void createActivite_vueSansAuthExposeUneListeVideDamis() {
        Model model = new ExtendedModelMap();

        String view = controller.createActivite(model, null);

        assertThat(view).isEqualTo("activity/create");
        assertThat((Object[]) model.getAttribute("typesSportifs")).contains(TypeSport.COURSE);
        assertThat((List<?>) model.getAttribute("amis")).isEmpty();
    }

    @Test
    void createActiviteForm_redirigeVersDetailEtPasseLaCommande() {
        Activite saved = Activite.builder().id(12L).build();
        when(activiteService.creerActivite(any(CreerActiviteCommand.class))).thenReturn(saved);

        String view = controller.createActiviteForm(
                1L,
                "Matinale",
                TypeSport.COURSE,
                LocalDate.of(2026, 4, 2),
                10.0,
                45,
                "Paris",
                4,
                List.of(3L, 4L));

        assertThat(view).isEqualTo("redirect:/activites/12/detail");

        ArgumentCaptor<CreerActiviteCommand> captor = ArgumentCaptor.forClass(CreerActiviteCommand.class);
        verify(activiteService).creerActivite(captor.capture());
        assertThat(captor.getValue().nom()).isEqualTo("Matinale");
        assertThat(captor.getValue().invitesIds()).containsExactly(3L, 4L);
    }

    @Test
    void createActiviteForm_redirigeVersFormulaireQuandErreur() {
        when(activiteService.creerActivite(any(CreerActiviteCommand.class)))
                .thenThrow(new IllegalArgumentException("bad"));

        String view = controller.createActiviteForm(
                1L,
                "Matinale",
                TypeSport.COURSE,
                LocalDate.of(2026, 4, 2),
                null,
                null,
                null,
                null,
                null);

        assertThat(view).isEqualTo("redirect:/activites/create?erreur=bad");
    }

    @Test
    void updateActivite_retourne404QuandAbsente() {
        when(activiteService.modifierActivite(eq(9L), any(ModifierActiviteRequest.class)))
                .thenThrow(new IllegalArgumentException("absente"));

        var response = controller.updateActivite(
                9L,
                new ModifierActiviteRequest("Acti", TypeSport.COURSE, 5.0, 30,
                        LocalDate.now(), "Paris", 5, List.of()));

        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void updateActivite_retourne200QuandTrouvee() {
        when(activiteService.modifierActivite(eq(9L), any(ModifierActiviteRequest.class))).thenReturn(activite);

        var response = controller.updateActivite(
                9L,
                new ModifierActiviteRequest("Acti", TypeSport.COURSE, 5.0, 30,
                        LocalDate.now(), "Paris", 5, List.of()));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(activite);
    }

    @Test
    void deleteActivite_retourne204QuandSupprimee() {
        var response = controller.deleteActivite(9L);

        verify(activiteService).supprimerActivite(9L);
        assertThat(response.getStatusCode().value()).isEqualTo(204);
    }

    @Test
    void deleteActivite_retourne404QuandAbsente() {
        org.mockito.Mockito.doThrow(new IllegalArgumentException("absente"))
                .when(activiteService).supprimerActivite(9L);

        var response = controller.deleteActivite(9L);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void getKilocalories_retourneValeur() {
        when(activiteService.calculerKilocalories(9L)).thenReturn(345.5);

        var response = controller.getKilocalories(9L);

        assertThat(response.getBody()).isEqualTo(345.5);
    }

    @Test
    void getKilocalories_retourne404QuandAbsente() {
        when(activiteService.calculerKilocalories(9L)).thenThrow(new IllegalArgumentException("absente"));

        var response = controller.getKilocalories(9L);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void recalculerMeteoEtCalories_retourneMessage() {
        when(activiteService.recalculerMeteoEtCaloriesPourToutesLesActivites()).thenReturn(3);

        var response = controller.recalculerMeteoEtCalories();

        assertThat(response.getBody()).isEqualTo("Mise à jour : 3 activité(s) enrichie(s).");
    }

    @Test
    void meteoPreview_retourneOkQuandInfoPresente() {
        when(openMeteoService.getWeatherForLocationAndDate("Paris", LocalDate.of(2026, 4, 2)))
                .thenReturn(new OpenMeteoService.WeatherInfo(18.5, "Nuageux"));

        var response = controller.meteoPreview("Paris", LocalDate.of(2026, 4, 2));

        assertThat(response.getBody()).isEqualTo(Map.of(
                "ok", true,
                "condition", "Nuageux",
                "temperature", 18.5));
    }

    @Test
    void meteoPreview_retourneOkFalseQuandInfoAbsente() {
        when(openMeteoService.getWeatherForLocationAndDate("Paris", LocalDate.of(2026, 4, 2)))
                .thenReturn(null);

        var response = controller.meteoPreview("Paris", LocalDate.of(2026, 4, 2));

        assertThat(response.getBody()).isEqualTo(Map.of("ok", false));
    }
}
