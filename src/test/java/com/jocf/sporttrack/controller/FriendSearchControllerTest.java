package com.jocf.sporttrack.controller;

import com.jocf.sporttrack.model.PrefSportive;
import com.jocf.sporttrack.model.TypeUtilisateur;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.repository.UtilisateurRepository;
import com.jocf.sporttrack.service.UtilisateurService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FriendSearchControllerTest {

    @Mock
    private UtilisateurService utilisateurService;

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @InjectMocks
    private FriendSearchController controller;

    private Utilisateur courant;
    private Utilisateur autre;
    private UsernamePasswordAuthenticationToken auth;

    @BeforeEach
    void setUp() {
        courant = utilisateur(1L, "Alice", "Doe", "alice@test.com");
        autre = utilisateur(2L, "Bob", "Smith", "bob@test.com");
        autre.setPrefSportives(List.of(new PrefSportive(7L, "Course", new ArrayList<>())));
        auth = new UsernamePasswordAuthenticationToken("alice@test.com", "x");
        when(utilisateurService.trouverParEmail("alice@test.com")).thenReturn(courant);
        when(utilisateurRepository.findAmiIdsByUtilisateurId(1L)).thenReturn(List.of());
        when(utilisateurRepository.findDemandesAmisEnvoyeesIdsByUtilisateurId(1L)).thenReturn(List.of());
        when(utilisateurRepository.findDemandesAmisRecuesByUtilisateurId(1L)).thenReturn(List.of());
        when(utilisateurRepository.findAllWithPrefSportives()).thenReturn(List.of(courant, autre));
        when(utilisateurRepository.rechercherPourReseau(1L, "bob")).thenReturn(List.of(autre));
    }

    @Test
    void afficherPageAmis_remplitModele() {
        Model model = new ExtendedModelMap();

        String view = controller.afficherPageAmis(auth, model);

        assertThat(view).isEqualTo("friend/search");
        assertThat(model.getAttribute("utilisateurCourant")).isSameAs(courant);
        assertThat(model.getAttribute("suggestionsVides")).isEqualTo(false);
    }

    @Test
    void rechercherAmis_retourneVueEtResultats() {
        Model model = new ExtendedModelMap();

        String view = controller.rechercherAmis(" bob ", auth, model);

        assertThat(view).isEqualTo("friend/search");
        assertThat(model.getAttribute("recherche")).isEqualTo("bob");
        assertThat((List<?>) model.getAttribute("resultatsRecherche")).hasSize(1);
    }

    @Test
    void envoyerDemandeAmi_refuseAutoAjout() {
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(courant));

        String view = controller.envoyerDemandeAmi(1L, null, null, auth, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/friend/search");
        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage"))
                .isEqualTo("Vous ne pouvez pas vous ajouter vous-meme.");
    }

    @Test
    void envoyerDemandeAmi_enregistreEtRedirigeVersProfil() {
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        when(utilisateurRepository.findById(2L)).thenReturn(Optional.of(autre));

        String view = controller.envoyerDemandeAmi(2L, null, "profile", auth, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/profile/2");
        assertThat(courant.getDemandesAmisEnvoyees()).containsExactly(autre);
        verify(utilisateurRepository).save(courant);
        assertThat(redirectAttributes.getFlashAttributes().get("successMessage"))
                .isEqualTo("Demande d'ami envoyee.");
    }

    @Test
    void accepterDemandeAmi_ajouteLesDeuxAmis() {
        autre.setDemandesAmisEnvoyees(new ArrayList<>(List.of(courant)));
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        when(utilisateurRepository.findById(2L)).thenReturn(Optional.of(autre));

        String view = controller.accepterDemandeAmi(2L, "bob", "friend", auth, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/friend?recherche=bob");
        assertThat(courant.getAmis()).containsExactly(autre);
        assertThat(autre.getAmis()).containsExactly(courant);
    }

    @Test
    void refuserDemandeAmi_retourneErreurSiIntrouvable() {
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        when(utilisateurRepository.findById(2L)).thenReturn(Optional.of(autre));

        String view = controller.refuserDemandeAmi(2L, null, null, auth, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/friend/search");
        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage"))
                .isEqualTo("Cette demande d'ami est introuvable.");
    }

    @Test
    void utilisateurCardView_peutEnvoyerDemande_dependsFlags() {
        var carte = new FriendSearchController.UtilisateurCardView(autre, "Bob Smith", "Course", false, false, false);
        var carteBloquee = new FriendSearchController.UtilisateurCardView(autre, "Bob Smith", "Course", true, false, false);

        assertThat(carte.peutEnvoyerDemande()).isTrue();
        assertThat(carteBloquee.peutEnvoyerDemande()).isFalse();
    }

    private static Utilisateur utilisateur(Long id, String prenom, String nom, String email) {
        return Utilisateur.builder()
                .id(id)
                .prenom(prenom)
                .nom(nom)
                .email(email)
                .motdepasse("x")
                .typeUtilisateur(TypeUtilisateur.UTILISATEUR)
                .amis(new ArrayList<>())
                .demandesAmisEnvoyees(new ArrayList<>())
                .prefSportives(new ArrayList<>())
                .build();
    }
}
