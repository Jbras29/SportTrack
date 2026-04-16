package com.jocf.sporttrack.model;

import com.jocf.sporttrack.dto.ModifierUtilisateurRequest;
import com.jocf.sporttrack.enumeration.TypeUtilisateur;
import com.jocf.sporttrack.repository.PrefSportiveRepository;
import com.jocf.sporttrack.repository.UtilisateurRepository;
import com.jocf.sporttrack.service.UtilisateurService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Couvre les méthodes métier déclarées dans {@link Utilisateur}, en s’appuyant sur
 * {@link UtilisateurService} pour la persistance et les mises à jour.
 * Les méthodes statiques d’XP ne sont pas invoquées par ce service : elles sont testées
 * directement sur {@link Utilisateur}.
 */
@ExtendWith(MockitoExtension.class)
class UtilisateurTest {

    private static final Long USER_ID = 1L;
    private static final String VALID_NOM = "Dupont";
    private static final String VALID_PRENOM = "Jean";
    private static final String VALID_EMAIL = "jean@example.com";
    private static final String VALID_MOTDEPASSE = "motdepasse";
    private static final String VALID_PHOTO_PROFIL = "/uploads/profiles/1.jpg";
    private static final String PLACEHOLDER_PHOTO = "/images/profile-placeholder.svg";
    private static final TypeUtilisateur VALID_TYPE = TypeUtilisateur.UTILISATEUR;

    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private PrefSportiveRepository prefSportiveRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ObjectProvider<AuthenticationConfiguration> authenticationConfigurationProvider;
    @Mock
    private AuthenticationConfiguration authenticationConfiguration;

    @InjectMocks
    private UtilisateurService utilisateurService;

    @BeforeEach
    void configureSave() {
        lenient().when(utilisateurRepository.save(any(Utilisateur.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(authenticationConfigurationProvider.getIfAvailable()).thenReturn(authenticationConfiguration);
    }

    private Utilisateur utilisateurMinimalPourCreation() {
        return Utilisateur.builder()
                .nom(VALID_NOM)
                .prenom(VALID_PRENOM)
                .email(VALID_EMAIL)
                .motdepasse(VALID_MOTDEPASSE)
                .typeUtilisateur(VALID_TYPE)
                .build();
    }

    @Test
    void cheminPhotoProfilAffichee_viaModifierPhotoProfil_photoRenseignee() {
        Utilisateur enBase = utilisateurMinimalPourCreation();
        enBase.setId(USER_ID);
        when(utilisateurRepository.findById(USER_ID)).thenReturn(Optional.of(enBase));

        utilisateurService.modifierPhotoProfil(USER_ID, VALID_PHOTO_PROFIL);

        ArgumentCaptor<Utilisateur> captor = ArgumentCaptor.forClass(Utilisateur.class);
        verify(utilisateurRepository).save(captor.capture());
        assertThat(captor.getValue().cheminPhotoProfilAffichee()).isEqualTo(VALID_PHOTO_PROFIL);
    }

    @Test
    void cheminPhotoProfilAffichee_viaModifierPhotoProfil_photoNull_utilisePlaceholder() {
        Utilisateur enBase = utilisateurMinimalPourCreation();
        enBase.setId(USER_ID);
        when(utilisateurRepository.findById(USER_ID)).thenReturn(Optional.of(enBase));

        utilisateurService.modifierPhotoProfil(USER_ID, null);

        ArgumentCaptor<Utilisateur> captor = ArgumentCaptor.forClass(Utilisateur.class);
        verify(utilisateurRepository).save(captor.capture());
        assertThat(captor.getValue().cheminPhotoProfilAffichee()).isEqualTo(PLACEHOLDER_PHOTO);
    }

    @Test
    void cheminPhotoProfilAffichee_viaModifierPhotoProfil_chaineVide_utilisePlaceholder() {
        Utilisateur enBase = utilisateurMinimalPourCreation();
        enBase.setId(USER_ID);
        when(utilisateurRepository.findById(USER_ID)).thenReturn(Optional.of(enBase));

        utilisateurService.modifierPhotoProfil(USER_ID, "");

        ArgumentCaptor<Utilisateur> captor = ArgumentCaptor.forClass(Utilisateur.class);
        verify(utilisateurRepository).save(captor.capture());
        assertThat(captor.getValue().cheminPhotoProfilAffichee()).isEqualTo(PLACEHOLDER_PHOTO);
    }

    @Test
    void getXpEffectif_viaCrediterExperience_xpInitialementNull() {
        Utilisateur u = utilisateurMinimalPourCreation();
        u.setId(USER_ID);
        u.setXp(null);

        Utilisateur credite = utilisateurService.crediterExperience(u, 42);

        assertThat(credite.getXpEffectif()).isEqualTo(42);
        assertThat(credite.getXp()).isEqualTo(42);
    }

    @Test
    void getXpEffectif_xpExplicitementZero() {
        Utilisateur u = utilisateurMinimalPourCreation();
        u.setXp(0);

        assertThat(u.getXpEffectif()).isZero();
    }

    @Test
    void crediterExperience_montantNegatifOuNul_nePersistePas() {
        Utilisateur u = utilisateurMinimalPourCreation();
        u.setId(USER_ID);
        u.setXp(10);

        Utilisateur r0 = utilisateurService.crediterExperience(u, 0);
        Utilisateur rNeg = utilisateurService.crediterExperience(u, -5);

        assertThat(r0).isSameAs(u);
        assertThat(rNeg).isSameAs(u);
        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    void getNiveauExperience_etJaugeXp_viaCrediterExperience() {
        Utilisateur u = utilisateurMinimalPourCreation();
        u.setId(USER_ID);
        u.setXp(null);
        utilisateurService.crediterExperience(u, 250);

        assertThat(u.getNiveauExperience()).isEqualTo(2);
        assertThat(u.getXpSeuilProchainNiveauExperience()).isEqualTo(500);
        assertThat(u.getXpDepuisSeuilNiveauExperience()).isEqualTo(150);
        assertThat(u.getPourcentageBarreExperience()).isEqualTo(37.5);
    }

    @Nested
    class NiveauExperience {

        /**
         * Seuils : {0, 100, 500, 1000, 3000, 5000} — chaque ligne teste la borne basse du palier attendu.
         */
        @ParameterizedTest(name = "xp={0} → niveau {1}")
        @CsvSource({
                "0, 1",
                "99, 1",
                "100, 2",
                "499, 2",
                "500, 3",
                "999, 3",
                "1000, 4",
                "2999, 4",
                "3000, 5",
                "4999, 5",
                "5000, 6",
                "7000, 6"
        })
        void getNiveauExperience_tousLesPaliers_viaCrediterExperience(int xpTotal, int niveauAttendu) {
            Utilisateur u = utilisateurMinimalPourCreation();
            u.setId(USER_ID);
            u.setXp(null);
            utilisateurService.crediterExperience(u, xpTotal);

            assertThat(u.getNiveauExperience()).isEqualTo(niveauAttendu);
        }

        @ParameterizedTest(name = "niveau déduit de xp={0} → seuil prochain = {1}")
        @CsvSource({
                "50, 100",
                "250, 500",
                "750, 1000",
                "1500, 3000",
                "4000, 5000",
                "5000, 5000",
                "8000, 5000"
        })
        void getXpSeuilProchainNiveauExperience_selonNiveau(int xpTotal, int seuilProchainAttendu) {
            Utilisateur u = utilisateurMinimalPourCreation();
            u.setId(USER_ID);
            u.setXp(null);
            utilisateurService.crediterExperience(u, xpTotal);

            assertThat(u.getXpSeuilProchainNiveauExperience()).isEqualTo(seuilProchainAttendu);
        }

        @Test
        void getXpDepuisSeuilNiveauExperience_auSeuilBas_duPalier_retourneZero() {
            Utilisateur u = utilisateurMinimalPourCreation();
            u.setId(USER_ID);
            u.setXp(null);
            utilisateurService.crediterExperience(u, 100);

            assertThat(u.getNiveauExperience()).isEqualTo(2);
            assertThat(u.getXpDepuisSeuilNiveauExperience()).isZero();
        }

        @ParameterizedTest(name = "xp={0} → pourcentage jauge = {1}")
        @CsvSource({
                "50, 50.0",
                "0, 0.0",
                "250, 37.5"
        })
        void getPourcentageBarreExperience_formuleEntrePaliers(int xpTotal, double pourcentageAttendu) {
            Utilisateur u = utilisateurMinimalPourCreation();
            u.setId(USER_ID);
            u.setXp(null);
            utilisateurService.crediterExperience(u, xpTotal);

            assertThat(u.getPourcentageBarreExperience()).isEqualTo(pourcentageAttendu);
        }
    }

    @Test
    void getPourcentageBarreExperience_palierMax_retourne100() {
        Utilisateur u = utilisateurMinimalPourCreation();
        u.setId(USER_ID);
        u.setXp(null);

        utilisateurService.crediterExperience(u, Utilisateur.SEUILS_XP_NIVEAU_EXPERIENCE[5]);

        assertThat(u.getNiveauExperience()).isEqualTo(6);
        assertThat(u.getPourcentageBarreExperience()).isEqualTo(100.0);
    }

    @Test
    void getHpNormalise_viaCreerUtilisateur_defaut100() {
        when(utilisateurRepository.existsByEmail(VALID_EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(VALID_MOTDEPASSE)).thenReturn("encoded");

        Utilisateur u = utilisateurMinimalPourCreation();
        u.setHp(null);

        Utilisateur cree = utilisateurService.creerUtilisateur(u);

        assertThat(cree.getHpNormalise()).isEqualTo(100);
    }

    @ParameterizedTest
    @CsvSource({
            "50, 50",
            "150, 100",
            "-10, 0"
    })
    void getHpNormalise_plancherEtPlafond(int hpBrut, int hpAffiche) {
        Utilisateur u = utilisateurMinimalPourCreation();
        u.setHp(hpBrut);

        assertThat(u.getHpNormalise()).isEqualTo(hpAffiche);
    }

    @Test
    void soustraireHp_estMort_estRestreintPourDefis_viaAppliquerPunitionChallenge() {
        Utilisateur u = utilisateurMinimalPourCreation();
        u.setId(USER_ID);
        u.setHp(100);
        when(utilisateurRepository.findById(USER_ID)).thenReturn(Optional.of(u));

        utilisateurService.appliquerPunitionChallenge(USER_ID, 100);

        assertThat(u.getHpNormalise()).isZero();
        assertThat(u.estMort()).isTrue();
        assertThat(u.estRestreintPourDefis()).isTrue();
    }

    @Test
    void appliquerPunitionChallenge_hpPartiellementRetire() {
        Utilisateur u = utilisateurMinimalPourCreation();
        u.setId(USER_ID);
        u.setHp(100);
        when(utilisateurRepository.findById(USER_ID)).thenReturn(Optional.of(u));

        utilisateurService.appliquerPunitionChallenge(USER_ID, 30);

        assertThat(u.getHpNormalise()).isEqualTo(70);
        assertThat(u.estMort()).isFalse();
        assertThat(u.estRestreintPourDefis()).isFalse();
    }

    @Test
    void soustraireHp_hpNull_traiteComme100_viaAppliquerPunitionChallenge() {
        Utilisateur u = utilisateurMinimalPourCreation();
        u.setId(USER_ID);
        u.setHp(null);
        when(utilisateurRepository.findById(USER_ID)).thenReturn(Optional.of(u));

        utilisateurService.appliquerPunitionChallenge(USER_ID, 40);

        assertThat(u.getHp()).isEqualTo(60);
        assertThat(u.getHpNormalise()).isEqualTo(60);
    }

    @Test
    void soustraireHp_pointsSuperieursAhp_plafonneAZero() {
        Utilisateur u = utilisateurMinimalPourCreation();
        u.setId(USER_ID);
        u.setHp(20);
        when(utilisateurRepository.findById(USER_ID)).thenReturn(Optional.of(u));

        utilisateurService.appliquerPunitionChallenge(USER_ID, 100);

        assertThat(u.getHp()).isZero();
    }

    @Test
    void estMort_et_estRestreintPourDefis_hpZero_sansCombat() {
        Utilisateur u = utilisateurMinimalPourCreation();
        u.setHp(0);

        assertThat(u.estMort()).isTrue();
        assertThat(u.estRestreintPourDefis()).isTrue();
    }

    @Test
    void estMort_hpNegatifNormaliseAZero() {
        Utilisateur u = utilisateurMinimalPourCreation();
        u.setHp(-3);

        assertThat(u.getHpNormalise()).isZero();
        assertThat(u.estMort()).isTrue();
    }

    @Test
    void distanceEnKmPourFormuleXp_nonExposeParUtilisateurService() {
        assertThat(Utilisateur.distanceEnKmPourFormuleXp(null)).isZero();
        assertThat(Utilisateur.distanceEnKmPourFormuleXp(0.0)).isZero();
        assertThat(Utilisateur.distanceEnKmPourFormuleXp(-5.0)).isZero();
        assertThat(Utilisateur.distanceEnKmPourFormuleXp(10.0)).isEqualTo(10.0);
        assertThat(Utilisateur.distanceEnKmPourFormuleXp(400.0)).isEqualTo(400.0);
        assertThat(Utilisateur.distanceEnKmPourFormuleXp(400.1)).isEqualTo(0.4001);
        assertThat(Utilisateur.distanceEnKmPourFormuleXp(5000.0)).isEqualTo(5.0);
    }

    @Test
    void calculerXpGagnePourActivite_nonExposeParUtilisateurService() {
        int xpMin = Utilisateur.calculerXpGagnePourActivite(0.0, 0);
        assertThat(xpMin).isGreaterThanOrEqualTo(1);

        int xpAvecEffort = Utilisateur.calculerXpGagnePourActivite(5.0, 30);
        assertThat(xpAvecEffort).isGreaterThan(xpMin);
    }

    @Test
    void calculerXpGagnePourActivite_valeursNegatives_rameneesAZero() {
        int xp = Utilisateur.calculerXpGagnePourActivite(-10.0, -5);
        assertThat(xp).isGreaterThanOrEqualTo(1);
    }

    @Test
    void modifierUtilisateur_photoVide_cheminPhotoProfilAffichee_placeholder() {
        Utilisateur enBase = utilisateurMinimalPourCreation();
        enBase.setId(USER_ID);
        enBase.setPhotoProfil(VALID_PHOTO_PROFIL);
        when(utilisateurRepository.findById(USER_ID)).thenReturn(Optional.of(enBase));

        ModifierUtilisateurRequest req = ModifierUtilisateurRequest.fromUtilisateur(enBase);
        req.setPhotoProfil("   ");

        utilisateurService.modifierUtilisateur(USER_ID, req);

        ArgumentCaptor<Utilisateur> captor = ArgumentCaptor.forClass(Utilisateur.class);
        verify(utilisateurRepository).save(captor.capture());
        assertThat(captor.getValue().cheminPhotoProfilAffichee()).isEqualTo(PLACEHOLDER_PHOTO);
    }
}
