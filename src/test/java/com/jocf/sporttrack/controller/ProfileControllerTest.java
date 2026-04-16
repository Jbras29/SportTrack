package com.jocf.sporttrack.controller;

import com.jocf.sporttrack.dto.ModifierUtilisateurRequest;
import com.jocf.sporttrack.model.PrefSportive;
import com.jocf.sporttrack.enumeration.NiveauPratiqueSportive;
import com.jocf.sporttrack.enumeration.TypeUtilisateur;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.repository.UtilisateurRepository;
import com.jocf.sporttrack.service.ActiviteService;
import com.jocf.sporttrack.service.PhotoProfilStorageService;
import com.jocf.sporttrack.service.UtilisateurService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileControllerTest {

    @Mock
    private UtilisateurService utilisateurService;

    @Mock
    private PhotoProfilStorageService photoProfilStorageService;

    @Mock
    private ActiviteService activiteService;

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @InjectMocks
    private ProfileController controller;

    private Utilisateur utilisateur;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        utilisateur = Utilisateur.builder()
                .id(1L)
                .nom("Doe")
                .prenom("Jane")
                .email("jane@test.com")
                .motdepasse("x")
                .typeUtilisateur(TypeUtilisateur.UTILISATEUR)
                .prefSportives(new ArrayList<>(List.of(new PrefSportive(2L, "Yoga", List.of()))))
                .amis(new ArrayList<>())
                .evenementsOrganises(new ArrayList<>())
                .evenementsParticipes(new ArrayList<>())
                .niveauPratiqueSportive(NiveauPratiqueSportive.DEBUTANT)
                .build();
        session = new MockHttpSession();
        session.setAttribute("utilisateurId", 1L);
    }

    @Test
    void editProfileForm_redirigeLoginSansSession() {
        String view = controller.editProfileForm(null, new MockHttpSession(), new ExtendedModelMap());

        assertThat(view).isEqualTo("redirect:/login");
    }

    @Test
    void editProfileForm_redirigeQuandLIdentifiantDemandeNeCorrespondPasALaSession() {
        String view = controller.editProfileForm(2L, session, new ExtendedModelMap());

        assertThat(view).isEqualTo("redirect:/profile/edit");
    }

    @Test
    void editProfileForm_chargeLeProfil() {
        when(utilisateurService.trouverParId(1L)).thenReturn(Optional.of(utilisateur));
        Model model = new ExtendedModelMap();

        String view = controller.editProfileForm(null, session, model);

        assertThat(view).isEqualTo("profile/edit");
        assertThat(model.getAttribute("utilisateur")).isSameAs(utilisateur);
        assertThat((Object[]) model.getAttribute("niveauxPratique")).contains(NiveauPratiqueSportive.DEBUTANT);
    }

    @Test
    void editProfileForm_renvoieErreurSiProfilIntrouvable() {
        when(utilisateurService.trouverParId(1L)).thenReturn(Optional.empty());
        ExtendedModelMap model = new ExtendedModelMap();

        assertThatThrownBy(() -> controller.editProfileForm(null, session, model))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Utilisateur introuvable");
    }

    @Test
    void editProfileForm_trieLesPreferencesAvecNomNull() {
        utilisateur.setPrefSportives(new ArrayList<>(List.of(
                new PrefSportive(3L, null, List.of()),
                new PrefSportive(4L, "Yoga", List.of()))));
        when(utilisateurService.trouverParId(1L)).thenReturn(Optional.of(utilisateur));

        Model model = new ExtendedModelMap();

        String view = controller.editProfileForm(null, session, model);

        assertThat(view).isEqualTo("profile/edit");
        assertThat(((Utilisateur) model.getAttribute("utilisateur")).getPrefSportives()).hasSize(2);
        assertThat(((Utilisateur) model.getAttribute("utilisateur")).getPrefSportives().get(0).getNom()).isNull();
    }

    @Test
    void editProfileSubmit_nettoieMotDePasseVide() {
        ModifierUtilisateurRequest form = ModifierUtilisateurRequest.fromUtilisateur(utilisateur);
        form.setMotdepasse("   ");

        String view = controller.editProfileSubmit(form, session);

        assertThat(view).isEqualTo("redirect:/profile/edit");
        assertThat(form.getMotdepasse()).isNull();
        verify(utilisateurService).modifierUtilisateur(1L, form);
    }

    @Test
    void editProfileSubmit_redirigeLoginQuandLaSessionEtLeFormulaireNeCorrespondentPas() {
        ModifierUtilisateurRequest form = ModifierUtilisateurRequest.fromUtilisateur(utilisateur);
        form.setId(2L);

        String view = controller.editProfileSubmit(form, session);

        assertThat(view).isEqualTo("redirect:/login");
    }

    @Test
    void editProfileSubmit_redirigeLoginQuandSessionAbsente() {
        ModifierUtilisateurRequest form = ModifierUtilisateurRequest.fromUtilisateur(utilisateur);
        MockHttpSession vide = new MockHttpSession();

        String view = controller.editProfileSubmit(form, vide);

        assertThat(view).isEqualTo("redirect:/login");
    }

    @Test
    void ajouterPreferenceSportive_stockeFlashSucces() {
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.ajouterPreferenceSportive(1L, "Course", session, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/profile/edit");
        assertThat(flashAttributes(redirectAttributes))
                .containsEntry("preferenceMessage", "Preference sportive ajoutee.");
    }

    @Test
    void ajouterPreferenceSportive_redirigeLoginSansSession() {
        String view = controller.ajouterPreferenceSportive(null, "Course", new MockHttpSession(), new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/login");
    }

    @Test
    void ajouterPreferenceSportive_retourneEditQuandIdNeCorrespondPas() {
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.ajouterPreferenceSportive(2L, "Course", session, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/profile/edit");
        assertThat(redirectAttributes.getFlashAttributes()).isEmpty();
    }

    @Test
    void ajouterPreferenceSportive_captreErreurDuService() {
        doThrow(new IllegalArgumentException("doublon"))
                .when(utilisateurService).ajouterPrefSportive(1L, "Course");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.ajouterPreferenceSportive(1L, "Course", session, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/profile/edit");
        assertThat(flashAttributes(redirectAttributes)).containsEntry("preferenceErreur", "doublon");
    }

    @Test
    void supprimerPreferenceSportive_stockeFlashErreur() {
        doThrow(new IllegalArgumentException("introuvable"))
                .when(utilisateurService).supprimerPrefSportive(1L, 2L);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.supprimerPreferenceSportive(2L, 1L, session, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/profile/edit");
        assertThat(flashAttributes(redirectAttributes)).containsEntry("preferenceErreur", "introuvable");
    }

    @Test
    void supprimerPreferenceSportive_stockeFlashSucces() {
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.supprimerPreferenceSportive(2L, 1L, session, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/profile/edit");
        assertThat(flashAttributes(redirectAttributes))
                .containsEntry("preferenceMessage", "Preference sportive supprimee.");
        verify(utilisateurService).supprimerPrefSportive(1L, 2L);
    }

    @Test
    void supprimerPreferenceSportive_redirigeLoginSansSession() {
        String view = controller.supprimerPreferenceSportive(2L, null, new MockHttpSession(), new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/login");
    }

    @Test
    void supprimerPreferenceSportive_retourneEditQuandIdNeCorrespondPas() {
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.supprimerPreferenceSportive(2L, 2L, session, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/profile/edit");
        assertThat(redirectAttributes.getFlashAttributes()).isEmpty();
    }

    @Test
    void televerserPhotoProfil_metAJourPhotoQuandSucces() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", new byte[] {1});
        when(photoProfilStorageService.enregistrerPhotoProfil(file, 1L)).thenReturn("/uploads/photo.png");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.televerserPhotoProfil(file, session, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/profile/edit?id=1");
        verify(utilisateurService).modifierPhotoProfil(1L, "/uploads/photo.png");
        assertThat(flashAttributes(redirectAttributes))
                .containsEntry("photoMessage", "Photo de profil mise à jour.");
    }

    @Test
    void televerserPhotoProfil_gereIOException() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", new byte[] {1});
        when(photoProfilStorageService.enregistrerPhotoProfil(file, 1L)).thenThrow(new IOException("io"));
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.televerserPhotoProfil(file, session, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/profile/edit?id=1");
        assertThat(flashAttributes(redirectAttributes))
                .containsEntry("photoErreur", "Impossible d'enregistrer la photo.");
    }

    @Test
    void televerserPhotoProfil_redirigeLoginSansSession() {
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", new byte[] {1});

        String view = controller.televerserPhotoProfil(file, new MockHttpSession(), new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/login");
    }

    @Test
    void televerserPhotoProfil_gereErreurMetier() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", new byte[] {1});
        when(photoProfilStorageService.enregistrerPhotoProfil(file, 1L))
                .thenThrow(new IllegalArgumentException("pas bon"));
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.televerserPhotoProfil(file, session, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/profile/edit?id=1");
        assertThat(flashAttributes(redirectAttributes)).containsEntry("photoErreur", "pas bon");
    }

    @Test
    void viewProfile_chargeLeProfilCourant() {
        when(utilisateurService.trouverParId(1L)).thenReturn(Optional.of(utilisateur));
        when(activiteService.recupererActivitesPourProfil(utilisateur)).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        String view = controller.viewProfile(session, model);

        assertThat(view).isEqualTo("profile/view");
        assertThat(model.getAttribute("profilCompletVisible")).isEqualTo(true);
    }

    @Test
    void viewProfile_redirigeSiProfilCourantIntrouvable() {
        when(utilisateurService.trouverParId(1L)).thenReturn(Optional.empty());
        ExtendedModelMap model = new ExtendedModelMap();

        assertThatThrownBy(() -> controller.viewProfile(session, model))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Utilisateur introuvable");
    }

    @Test
    void viewProfile_redirigeLoginSansSession() {
        String view = controller.viewProfile(new MockHttpSession(), new ExtendedModelMap());

        assertThat(view).isEqualTo("redirect:/login");
    }

    @Test
    void viewProfileAutre_redirigeLoginSansSession() {
        String view = controller.viewProfile(2L, new MockHttpSession(), new ExtendedModelMap());

        assertThat(view).isEqualTo("redirect:/login");
    }

    @Test
    void viewProfileAutre_cacheProfilPriveEtExposeDrapeaux() {
        Utilisateur prive = Utilisateur.builder()
                .id(2L)
                .nom("Smith")
                .prenom("Bob")
                .email("bob@test.com")
                .motdepasse("x")
                .typeUtilisateur(TypeUtilisateur.UTILISATEUR)
                .comptePrive(true)
                .amis(new ArrayList<>())
                .evenementsOrganises(new ArrayList<>())
                .evenementsParticipes(new ArrayList<>())
                .build();
        when(utilisateurService.trouverParId(2L)).thenReturn(Optional.of(prive));
        when(utilisateurService.findByIdWithAmis(1L)).thenReturn(utilisateur);
        when(utilisateurRepository.findDemandesAmisEnvoyeesIdsByUtilisateurId(1L)).thenReturn(List.of());
        when(utilisateurRepository.findDemandesAmisRecuesByUtilisateurId(1L)).thenReturn(List.of());

        Model model = new ExtendedModelMap();
        String view = controller.viewProfile(2L, session, model);

        assertThat(view).isEqualTo("profile/view");
        assertThat(model.getAttribute("profilCompletVisible")).isEqualTo(false);
        assertThat(model.getAttribute("peutEnvoyerDemandeAmiProfil")).isEqualTo(true);
    }

    @Test
    void viewProfileAutre_rendLeProfilVisibleQuandLUtilisateurEstAmi() {
        Utilisateur prive = Utilisateur.builder()
                .id(2L)
                .nom("Smith")
                .prenom("Bob")
                .email("bob@test.com")
                .motdepasse("x")
                .typeUtilisateur(TypeUtilisateur.UTILISATEUR)
                .comptePrive(true)
                .amis(new ArrayList<>())
                .evenementsOrganises(new ArrayList<>())
                .evenementsParticipes(new ArrayList<>())
                .build();
        Utilisateur connecte = Utilisateur.builder()
                .id(1L)
                .nom("Doe")
                .prenom("Jane")
                .email("jane@test.com")
                .motdepasse("x")
                .typeUtilisateur(TypeUtilisateur.UTILISATEUR)
                .amis(new ArrayList<>(List.of(prive)))
                .build();
        when(utilisateurService.trouverParId(2L)).thenReturn(Optional.of(prive));
        when(utilisateurService.findByIdWithAmis(1L)).thenReturn(connecte);
        when(activiteService.recupererActivitesPourProfil(prive)).thenReturn(List.of());

        Model model = new ExtendedModelMap();
        String view = controller.viewProfile(2L, session, model);

        assertThat(view).isEqualTo("profile/view");
        assertThat(model.getAttribute("estAmi")).isEqualTo(true);
        assertThat(model.getAttribute("profilCompletVisible")).isEqualTo(true);
    }

    @Test
    void viewProfileAutre_redirigeSiProfilCibleIntrouvable() {
        when(utilisateurService.trouverParId(2L)).thenReturn(Optional.empty());
        ExtendedModelMap model = new ExtendedModelMap();

        assertThatThrownBy(() -> controller.viewProfile(2L, session, model))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Utilisateur introuvable");
    }

    @Test
    void viewProfileAutre_rendLeProfilVisibleQuandIlEstPublic() {
        Utilisateur publicProfile = Utilisateur.builder()
                .id(2L)
                .nom("Smith")
                .prenom("Bob")
                .email("bob@test.com")
                .motdepasse("x")
                .typeUtilisateur(TypeUtilisateur.UTILISATEUR)
                .comptePrive(false)
                .amis(new ArrayList<>())
                .evenementsOrganises(new ArrayList<>())
                .evenementsParticipes(new ArrayList<>())
                .build();
        when(utilisateurService.trouverParId(2L)).thenReturn(Optional.of(publicProfile));
        when(utilisateurService.findByIdWithAmis(1L)).thenReturn(utilisateur);
        when(activiteService.recupererActivitesPourProfil(publicProfile)).thenReturn(List.of());

        Model model = new ExtendedModelMap();
        String view = controller.viewProfile(2L, session, model);

        assertThat(view).isEqualTo("profile/view");
        assertThat(model.getAttribute("profilCompletVisible")).isEqualTo(true);
        assertThat(model.getAttribute("estAmi")).isEqualTo(false);
    }

    @Test
    void viewProfileAutre_cacheProfilPriveEtExposeLesChampsDeDemande() {
        Utilisateur prive = Utilisateur.builder()
                .id(2L)
                .nom("Smith")
                .prenom("Bob")
                .email("bob@test.com")
                .motdepasse("x")
                .typeUtilisateur(TypeUtilisateur.UTILISATEUR)
                .comptePrive(true)
                .amis(new ArrayList<>())
                .evenementsOrganises(new ArrayList<>())
                .evenementsParticipes(new ArrayList<>())
                .build();
        when(utilisateurService.trouverParId(2L)).thenReturn(Optional.of(prive));
        when(utilisateurService.findByIdWithAmis(1L)).thenReturn(utilisateur);
        when(utilisateurRepository.findDemandesAmisEnvoyeesIdsByUtilisateurId(1L)).thenReturn(List.of(2L));
        when(utilisateurRepository.findDemandesAmisRecuesByUtilisateurId(1L)).thenReturn(List.of());

        Model model = new ExtendedModelMap();
        String view = controller.viewProfile(2L, session, model);

        assertThat(view).isEqualTo("profile/view");
        assertThat(model.getAttribute("profilCompletVisible")).isEqualTo(false);
        assertThat(model.getAttribute("demandeEnvoyeeProfil")).isEqualTo(true);
        assertThat(model.getAttribute("peutEnvoyerDemandeAmiProfil")).isEqualTo(false);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> flashAttributes(RedirectAttributesModelMap redirectAttributes) {
        return new java.util.HashMap<>((Map<String, Object>) (Map<?, ?>) redirectAttributes.getFlashAttributes());
    }
}
