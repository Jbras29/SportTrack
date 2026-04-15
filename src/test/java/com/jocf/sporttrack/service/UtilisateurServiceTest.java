package com.jocf.sporttrack.service;

import com.jocf.sporttrack.dto.ModifierUtilisateurRequest;
import com.jocf.sporttrack.model.PrefSportive;
import com.jocf.sporttrack.enumeration.TypeUtilisateur;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.repository.PrefSportiveRepository;
import com.jocf.sporttrack.repository.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UtilisateurServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long AMI_ID = 2L;
    private static final Long PREF_ID = 10L;
    private static final String EMAIL = "jean@test.com";
    private static final String MOTDEPASSE_HASH = "hash-existant";
    private static final String MOTDEPASSE_CLAIR = "secret";
    private static final String NOM = "Dupont";
    private static final String PRENOM = "Jean";

    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private PrefSportiveRepository prefSportiveRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationConfiguration authenticationConfiguration;

    @InjectMocks
    private UtilisateurService utilisateurService;

    @BeforeEach
    void lenientSave() {
        lenient().when(utilisateurRepository.save(any(Utilisateur.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(prefSportiveRepository.save(any(PrefSportive.class)))
                .thenAnswer(invocation -> {
                    PrefSportive p = invocation.getArgument(0);
                    if (p.getId() == null) {
                        p.setId(99L);
                    }
                    return p;
                });
    }

    private Utilisateur utilisateurEnBase() {
        Utilisateur u = Utilisateur.builder()
                .id(USER_ID)
                .nom(NOM)
                .prenom(PRENOM)
                .email(EMAIL)
                .motdepasse(MOTDEPASSE_HASH)
                .typeUtilisateur(TypeUtilisateur.UTILISATEUR)
                .comptePrive(false)
                .build();
        u.setPrefSportives(new ArrayList<>());
        u.setAmis(new ArrayList<>());
        return u;
    }

    private ModifierUtilisateurRequest requeteModificationMinimale() {
        ModifierUtilisateurRequest req = new ModifierUtilisateurRequest();
        req.setNom(NOM);
        req.setPrenom(PRENOM);
        req.setEmail(EMAIL);
        req.setMotdepasse(null);
        req.setComptePrive(false);
        return req;
    }

    @Nested
    class ModifierUtilisateur {

        @Test
        void utilisateurIntrouvable() {
            when(utilisateurRepository.findById(USER_ID)).thenReturn(Optional.empty());
            ModifierUtilisateurRequest req = requeteModificationMinimale();

            assertThatThrownBy(() -> utilisateurService.modifierUtilisateur(USER_ID, req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Utilisateur introuvable");
        }

        @Test
        void motDePasseNull_conserveAncienHash() {
            Utilisateur u = utilisateurEnBase();
            when(utilisateurRepository.findById(USER_ID)).thenReturn(Optional.of(u));
            ModifierUtilisateurRequest req = requeteModificationMinimale();
            req.setMotdepasse(null);

            utilisateurService.modifierUtilisateur(USER_ID, req);

            assertThat(u.getMotdepasse()).isEqualTo(MOTDEPASSE_HASH);
            verify(passwordEncoder, never()).encode(any());
        }

        @Test
        void motDePasseBlanc_conserveAncienHash() {
            Utilisateur u = utilisateurEnBase();
            when(utilisateurRepository.findById(USER_ID)).thenReturn(Optional.of(u));
            ModifierUtilisateurRequest req = requeteModificationMinimale();
            req.setMotdepasse("   \t");

            utilisateurService.modifierUtilisateur(USER_ID, req);

            assertThat(u.getMotdepasse()).isEqualTo(MOTDEPASSE_HASH);
            verify(passwordEncoder, never()).encode(any());
        }

        @Test
        void motDePasseNonVide_encodeEtRemplace() {
            Utilisateur u = utilisateurEnBase();
            when(utilisateurRepository.findById(USER_ID)).thenReturn(Optional.of(u));
            when(passwordEncoder.encode(MOTDEPASSE_CLAIR)).thenReturn("nouveau-hash");
            ModifierUtilisateurRequest req = requeteModificationMinimale();
            req.setMotdepasse(MOTDEPASSE_CLAIR);

            utilisateurService.modifierUtilisateur(USER_ID, req);

            assertThat(u.getMotdepasse()).isEqualTo("nouveau-hash");
            verify(passwordEncoder).encode(MOTDEPASSE_CLAIR);
        }

        @Test
        void photoProfilNull_neModifiePasLaPhotoExistante() {
            Utilisateur u = utilisateurEnBase();
            u.setPhotoProfil("/ancienne.jpg");
            when(utilisateurRepository.findById(USER_ID)).thenReturn(Optional.of(u));
            ModifierUtilisateurRequest req = requeteModificationMinimale();
            req.setPhotoProfil(null);

            utilisateurService.modifierUtilisateur(USER_ID, req);

            assertThat(u.getPhotoProfil()).isEqualTo("/ancienne.jpg");
        }

        @Test
        void photoProfilNonNull_maisBlanc_effacePhoto() {
            Utilisateur u = utilisateurEnBase();
            u.setPhotoProfil("/ancienne.jpg");
            when(utilisateurRepository.findById(USER_ID)).thenReturn(Optional.of(u));
            ModifierUtilisateurRequest req = requeteModificationMinimale();
            req.setPhotoProfil("   ");

            utilisateurService.modifierUtilisateur(USER_ID, req);

            assertThat(u.getPhotoProfil()).isNull();
        }

        @Test
        void photoProfilNonNull_valeurRemplace() {
            Utilisateur u = utilisateurEnBase();
            when(utilisateurRepository.findById(USER_ID)).thenReturn(Optional.of(u));
            ModifierUtilisateurRequest req = requeteModificationMinimale();
            req.setPhotoProfil("/nouvelle.jpg");

            utilisateurService.modifierUtilisateur(USER_ID, req);

            assertThat(u.getPhotoProfil()).isEqualTo("/nouvelle.jpg");
        }

        @Test
        void prefSportivesIdsNull_neReinitialisePasLaListe() {
            Utilisateur u = utilisateurEnBase();
            PrefSportive pref = PrefSportive.builder().id(PREF_ID).nom("Course").build();
            u.getPrefSportives().add(pref);
            when(utilisateurRepository.findById(USER_ID)).thenReturn(Optional.of(u));
            ModifierUtilisateurRequest req = requeteModificationMinimale();
            req.setPrefSportivesIds(null);

            utilisateurService.modifierUtilisateur(USER_ID, req);

            assertThat(u.getPrefSportives()).containsExactly(pref);
            verify(prefSportiveRepository, never()).findById(any());
        }

        @Test
        void prefSportivesIdsVide_effaceEtNeChargeRien() {
            Utilisateur u = utilisateurEnBase();
            u.getPrefSportives().add(PrefSportive.builder().id(PREF_ID).nom("Course").build());
            when(utilisateurRepository.findById(USER_ID)).thenReturn(Optional.of(u));
            ModifierUtilisateurRequest req = requeteModificationMinimale();
            req.setPrefSportivesIds(Collections.emptyList());

            utilisateurService.modifierUtilisateur(USER_ID, req);

            assertThat(u.getPrefSportives()).isEmpty();
        }

        @Test
        void prefSportivesIds_contientNull_ignoreNull_etAjouteSiPresent() {
            Utilisateur u = utilisateurEnBase();
            PrefSportive pref = PrefSportive.builder().id(PREF_ID).nom("Course").build();
            when(utilisateurRepository.findById(USER_ID)).thenReturn(Optional.of(u));
            when(prefSportiveRepository.findById(PREF_ID)).thenReturn(Optional.of(pref));
            ModifierUtilisateurRequest req = requeteModificationMinimale();
            req.setPrefSportivesIds(new ArrayList<>(Arrays.asList(null, PREF_ID)));

            utilisateurService.modifierUtilisateur(USER_ID, req);

            assertThat(u.getPrefSportives()).containsExactly(pref);
            verify(prefSportiveRepository, never()).findById(null);
        }

        @Test
        void prefSportivesIds_prefIntrouvable_neAjoutePas() {
            Utilisateur u = utilisateurEnBase();
            when(utilisateurRepository.findById(USER_ID)).thenReturn(Optional.of(u));
            when(prefSportiveRepository.findById(PREF_ID)).thenReturn(Optional.empty());
            ModifierUtilisateurRequest req = requeteModificationMinimale();
            req.setPrefSportivesIds(List.of(PREF_ID));

            utilisateurService.modifierUtilisateur(USER_ID, req);

            assertThat(u.getPrefSportives()).isEmpty();
        }
    }

    @Nested
    class AjouterPrefSportive {

        @Test
        void utilisateurIntrouvable() {
            when(utilisateurRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> utilisateurService.ajouterPrefSportive(USER_ID, "Course"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Utilisateur introuvable");
        }

        @Test
        void nomNull() {
            when(utilisateurRepository.findById(USER_ID)).thenReturn(Optional.of(utilisateurEnBase()));

            assertThatThrownBy(() -> utilisateurService.ajouterPrefSportive(USER_ID, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("obligatoire");
        }

        @Test
        void nomBlancApresNormalisation() {
            when(utilisateurRepository.findById(USER_ID)).thenReturn(Optional.of(utilisateurEnBase()));

            assertThatThrownBy(() -> utilisateurService.ajouterPrefSportive(USER_ID, "   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("obligatoire");
        }

        @Test
        void dejaAssociee() {
            Utilisateur u = utilisateurEnBase();
            u.getPrefSportives().add(PrefSportive.builder().id(1L).nom("Course").build());
            when(utilisateurRepository.findById(USER_ID)).thenReturn(Optional.of(u));

            assertThatThrownBy(() -> utilisateurService.ajouterPrefSportive(USER_ID, "course"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("existe deja");
        }

        @Test
        void preferenceExistanteEnBase_reutiliseSansSaveNouvellePref() {
            Utilisateur u = utilisateurEnBase();
            PrefSportive existante = PrefSportive.builder().id(5L).nom("Course").build();
            when(utilisateurRepository.findById(USER_ID)).thenReturn(Optional.of(u));
            when(prefSportiveRepository.findByNomIgnoreCase("Course")).thenReturn(Optional.of(existante));

            utilisateurService.ajouterPrefSportive(USER_ID, "  Course  ");

            assertThat(u.getPrefSportives()).containsExactly(existante);
            verify(prefSportiveRepository, never()).save(any(PrefSportive.class));
        }

        @Test
        void preferenceAbsente_creeEtPersiste() {
            Utilisateur u = utilisateurEnBase();
            when(utilisateurRepository.findById(USER_ID)).thenReturn(Optional.of(u));
            when(prefSportiveRepository.findByNomIgnoreCase("Natation")).thenReturn(Optional.empty());

            utilisateurService.ajouterPrefSportive(USER_ID, "Natation");

            ArgumentCaptor<PrefSportive> captor = ArgumentCaptor.forClass(PrefSportive.class);
            verify(prefSportiveRepository).save(captor.capture());
            assertThat(captor.getValue().getNom()).isEqualTo("Natation");
            assertThat(u.getPrefSportives()).hasSize(1);
        }
    }

    @Nested
    class SupprimerPrefSportive {

        @Test
        void utilisateurIntrouvable() {
            when(utilisateurRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> utilisateurService.supprimerPrefSportive(USER_ID, PREF_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Utilisateur introuvable");
        }

        @Test
        void preferencePasSurLeProfil() {
            Utilisateur u = utilisateurEnBase();
            when(utilisateurRepository.findById(USER_ID)).thenReturn(Optional.of(u));

            assertThatThrownBy(() -> utilisateurService.supprimerPrefSportive(USER_ID, PREF_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("introuvable sur ce profil");
        }

        @Test
        void supprimeEtSauvegarde() {
            Utilisateur u = utilisateurEnBase();
            PrefSportive pref = PrefSportive.builder().id(PREF_ID).nom("Course").build();
            u.getPrefSportives().add(pref);
            when(utilisateurRepository.findById(USER_ID)).thenReturn(Optional.of(u));

            utilisateurService.supprimerPrefSportive(USER_ID, PREF_ID);

            assertThat(u.getPrefSportives()).isEmpty();
            verify(utilisateurRepository).save(u);
        }
    }

    @Nested
    class AccesEtPhoto {

        @Test
        void modifierPhotoProfil_introuvable() {
            when(utilisateurRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> utilisateurService.modifierPhotoProfil(USER_ID, "/p.jpg"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Utilisateur introuvable");
        }

        @Test
        void enregistrerDerniereConsultation_introuvable() {
            when(utilisateurRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> utilisateurService.enregistrerDerniereConsultationNotifications(USER_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Utilisateur introuvable");
        }
    }

    @Nested
    class CrediterExperience {

        @Test
        void montantNegatifOuNul_nePersistePas() {
            Utilisateur u = utilisateurEnBase();
            u.setXp(10);

            assertThat(utilisateurService.crediterExperience(u, 0)).isSameAs(u);
            assertThat(utilisateurService.crediterExperience(u, -3)).isSameAs(u);
            verify(utilisateurRepository, never()).save(any());
        }

        @Test
        void montantPositif_persiste() {
            Utilisateur u = utilisateurEnBase();
            u.setXp(null);

            utilisateurService.crediterExperience(u, 15);

            assertThat(u.getXp()).isEqualTo(15);
            verify(utilisateurRepository).save(u);
        }
    }

    @Nested
    class SupprimerUtilisateur {

        @Test
        void utilisateurInexistant() {
            when(utilisateurRepository.existsById(USER_ID)).thenReturn(false);

            assertThatThrownBy(() -> utilisateurService.supprimerUtilisateur(USER_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Utilisateur introuvable");
        }

        @Test
        void utilisateurExistant_supprime() {
            when(utilisateurRepository.existsById(USER_ID)).thenReturn(true);

            utilisateurService.supprimerUtilisateur(USER_ID);

            verify(utilisateurRepository).deleteById(USER_ID);
        }
    }

    @Nested
    class CreerUtilisateur {

        @Test
        void emailDejaUtilise() {
            when(utilisateurRepository.existsByEmail(EMAIL)).thenReturn(true);
            Utilisateur u = utilisateurEnBase();
            u.setId(null);

            assertThatThrownBy(() -> utilisateurService.creerUtilisateur(u))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("deja utilise");
        }

        @Test
        void xpEtHpNull_defaut0et100() {
            when(utilisateurRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(MOTDEPASSE_CLAIR)).thenReturn("h");
            Utilisateur u = utilisateurEnBase();
            u.setId(null);
            u.setMotdepasse(MOTDEPASSE_CLAIR);
            u.setXp(null);
            u.setHp(null);

            utilisateurService.creerUtilisateur(u);

            assertThat(u.getXp()).isZero();
            assertThat(u.getHp()).isEqualTo(100);
            assertThat(u.getId()).isNull();
        }

        @Test
        void xpEtHpRenseignes_conserves() {
            when(utilisateurRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(MOTDEPASSE_CLAIR)).thenReturn("h");
            Utilisateur u = utilisateurEnBase();
            u.setId(null);
            u.setMotdepasse(MOTDEPASSE_CLAIR);
            u.setXp(50);
            u.setHp(80);

            utilisateurService.creerUtilisateur(u);

            assertThat(u.getXp()).isEqualTo(50);
            assertThat(u.getHp()).isEqualTo(80);
        }
    }

    @Nested
    class TrouverEtConnexion {

        @Test
        void trouverParEmail_introuvable() {
            when(utilisateurRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> utilisateurService.trouverParEmail(EMAIL))
                    .isInstanceOf(UsernameNotFoundException.class);
        }

        @Test
        void trouverParEmailAvecAmis_introuvable() {
            when(utilisateurRepository.findByEmailWithAmis(EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> utilisateurService.trouverParEmailAvecAmis(EMAIL))
                    .isInstanceOf(UsernameNotFoundException.class);
        }

        @Test
        void findById_introuvable() {
            when(utilisateurRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> utilisateurService.findById(USER_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("non trouvé");
        }

        @Test
        void findByIdWithAmis_introuvable() {
            when(utilisateurRepository.findByIdWithAmis(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> utilisateurService.findByIdWithAmis(USER_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("non trouvé");
        }

        @Test
        void connecter_authentifiePuisChargeParEmail() {
            Utilisateur u = utilisateurEnBase();
            AuthenticationManager authManager = mock(AuthenticationManager.class);
            when(authenticationConfiguration.getAuthenticationManager()).thenReturn(authManager);
            Authentication auth = mock(Authentication.class);
            when(auth.getName()).thenReturn(EMAIL);
            when(authManager.authenticate(any())).thenReturn(auth);
            when(utilisateurRepository.findByEmail(EMAIL)).thenReturn(Optional.of(u));

            Utilisateur resultat = utilisateurService.connecter(EMAIL, MOTDEPASSE_CLAIR);

            assertThat(resultat).isSameAs(u);
            verify(authManager).authenticate(any());
        }

        @Test
        void getAuthenticationManager_echec_initialisation() {
            when(authenticationConfiguration.getAuthenticationManager())
                    .thenThrow(new IllegalStateException("boom"));

            assertThatThrownBy(() -> utilisateurService.connecter(EMAIL, MOTDEPASSE_CLAIR))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Impossible d'initialiser");
        }

        @Test
        void loadUserByUsername_construitUserDetails() {
            Utilisateur u = utilisateurEnBase();
            when(utilisateurRepository.findByEmail(EMAIL)).thenReturn(Optional.of(u));

            UserDetails details = utilisateurService.loadUserByUsername(EMAIL);

            assertThat(details.getUsername()).isEqualTo(EMAIL);
            assertThat(details.getPassword()).isEqualTo(MOTDEPASSE_HASH);
            assertThat(details.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
        }
    }

    @Nested
    class DelegationsSimples {

        @Test
        void recupererTousLesUtilisateurs() {
            List<Utilisateur> liste = List.of(utilisateurEnBase());
            when(utilisateurRepository.findAll()).thenReturn(liste);

            assertThat(utilisateurService.recupererTousLesUtilisateurs()).isSameAs(liste);
        }

        @Test
        void trouverParId() {
            when(utilisateurRepository.findById(USER_ID)).thenReturn(Optional.of(utilisateurEnBase()));

            assertThat(utilisateurService.trouverParId(USER_ID)).isPresent();
        }

        @Test
        void rechercherParNom() {
            when(utilisateurRepository.findByPrenomContainingIgnoreCase("Jean")).thenReturn(List.of());

            assertThat(utilisateurService.rechercherParNom("Jean")).isEmpty();
        }
    }

    @Nested
    class PeutAfficherIdentiteVers {

        @Test
        void sujetNull() {
            assertThat(utilisateurService.peutAfficherIdentiteVers(null, USER_ID)).isFalse();
        }

        @Test
        void visiteurNull() {
            assertThat(utilisateurService.peutAfficherIdentiteVers(utilisateurEnBase(), null)).isFalse();
        }

        @Test
        void visiteurEstLeSujet() {
            Utilisateur sujet = utilisateurEnBase();

            assertThat(utilisateurService.peutAfficherIdentiteVers(sujet, USER_ID)).isTrue();
        }

        @Test
        void profilPublic() {
            Utilisateur sujet = utilisateurEnBase();
            sujet.setComptePrive(false);

            assertThat(utilisateurService.peutAfficherIdentiteVers(sujet, 999L)).isTrue();
        }

        @Test
        void profilPrive_nonAmi() {
            Utilisateur sujet = utilisateurEnBase();
            sujet.setComptePrive(true);
            when(utilisateurRepository.findAmiIdsByUtilisateurId(AMI_ID)).thenReturn(List.of());

            assertThat(utilisateurService.peutAfficherIdentiteVers(sujet, AMI_ID)).isFalse();
        }

        @Test
        void profilPrive_ami() {
            Utilisateur sujet = utilisateurEnBase();
            sujet.setComptePrive(true);
            when(utilisateurRepository.findAmiIdsByUtilisateurId(AMI_ID)).thenReturn(List.of(USER_ID));

            assertThat(utilisateurService.peutAfficherIdentiteVers(sujet, AMI_ID)).isTrue();
        }
    }

    @Nested
    class SupprimerAmi {

        @Test
        void utilisateurIntrouvable() {
            when(utilisateurRepository.findByIdWithAmis(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> utilisateurService.supprimerAmi(USER_ID, AMI_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Utilisateur non trouvé");
        }

        @Test
        void amiIntrouvable() {
            when(utilisateurRepository.findByIdWithAmis(USER_ID)).thenReturn(Optional.of(utilisateurEnBase()));
            when(utilisateurRepository.findByIdWithAmis(AMI_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> utilisateurService.supprimerAmi(USER_ID, AMI_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Ami non trouvé");
        }

        @Test
        void retireLesDeuxCotes() {
            Utilisateur u = utilisateurEnBase();
            Utilisateur ami = Utilisateur.builder()
                    .id(AMI_ID)
                    .nom("A")
                    .prenom("B")
                    .email("ami@test.com")
                    .motdepasse("x")
                    .typeUtilisateur(TypeUtilisateur.UTILISATEUR)
                    .build();
            u.setAmis(new ArrayList<>(List.of(ami)));
            ami.setAmis(new ArrayList<>(List.of(u)));
            when(utilisateurRepository.findByIdWithAmis(USER_ID)).thenReturn(Optional.of(u));
            when(utilisateurRepository.findByIdWithAmis(AMI_ID)).thenReturn(Optional.of(ami));

            utilisateurService.supprimerAmi(USER_ID, AMI_ID);

            assertThat(u.getAmis()).isEmpty();
            assertThat(ami.getAmis()).isEmpty();
            verify(utilisateurRepository).save(u);
            verify(utilisateurRepository).save(ami);
        }
    }

    @Nested
    class AppliquerPunitionChallenge {

        @Test
        void utilisateurInexistant_neSauvegardePas() {
            when(utilisateurRepository.findById(USER_ID)).thenReturn(Optional.empty());

            utilisateurService.appliquerPunitionChallenge(USER_ID, 10);

            verify(utilisateurRepository, never()).save(any());
        }

        @Test
        void utilisateurExistant_soustraitEtSauve() {
            Utilisateur u = utilisateurEnBase();
            u.setHp(100);
            when(utilisateurRepository.findById(USER_ID)).thenReturn(Optional.of(u));

            utilisateurService.appliquerPunitionChallenge(USER_ID, 25);

            assertThat(u.getHp()).isEqualTo(75);
            verify(utilisateurRepository).save(u);
        }
    }

    @Nested
    class ListerAmisTries {

        @Test
        void trieParPrenomPuisNom() {
            Utilisateur u = utilisateurEnBase();
            Utilisateur a = Utilisateur.builder().id(3L).nom("Z").prenom("alpha").email("a@x.com").motdepasse("x").typeUtilisateur(TypeUtilisateur.UTILISATEUR).build();
            Utilisateur b = Utilisateur.builder().id(4L).nom("A").prenom("Alpha").email("b@x.com").motdepasse("x").typeUtilisateur(TypeUtilisateur.UTILISATEUR).build();
            u.setAmis(new ArrayList<>(List.of(b, a)));

            List<Utilisateur> tries = utilisateurService.listerAmisTries(u);

            assertThat(tries).containsExactly(b, a);
        }
    }
}
