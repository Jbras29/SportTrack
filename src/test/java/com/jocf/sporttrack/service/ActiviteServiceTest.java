package com.jocf.sporttrack.service;

import com.jocf.sporttrack.dto.CreerActiviteCommand;
import com.jocf.sporttrack.dto.ModifierActiviteRequest;
import com.jocf.sporttrack.model.Activite;
import com.jocf.sporttrack.enumeration.TypeSport;
import com.jocf.sporttrack.enumeration.TypeUtilisateur;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.repository.ActiviteRepository;
import com.jocf.sporttrack.repository.UtilisateurRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActiviteServiceTest {

    @Mock
    private ActiviteRepository activiteRepository;

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private UtilisateurService utilisateurService;

    @Mock
    private ActiviteBadgeEvaluationService activiteBadgeEvaluationService;

    @Mock
    private OpenMeteoService openMeteoService;

    @InjectMocks
    private ActiviteService service;

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
                .poids(70.0)
                .amis(new ArrayList<>())
                .build();
    }

    @Test
    void recupererToutesLesActivites_etTrouverParId_deleguentAuRepository() {
        Activite activite = Activite.builder().id(3L).utilisateur(utilisateur).typeSport(TypeSport.COURSE).date(LocalDate.now()).build();
        when(activiteRepository.findAll()).thenReturn(List.of(activite));
        when(activiteRepository.findById(3L)).thenReturn(Optional.of(activite));

        assertThat(service.recupererToutesLesActivites()).containsExactly(activite);
        assertThat(service.trouverParId(3L)).contains(activite);
    }

    @Test
    void recupererSuggestionsLocations_delegueAOpenMeteo() {
        when(openMeteoService.suggestLocations("Pa", 2)).thenReturn(List.of("Paris", "Pau"));

        List<String> suggestions = service.recupererSuggestionsLocations(" Pa ", 2);

        assertThat(suggestions).containsExactly("Paris", "Pau");
        verify(openMeteoService).suggestLocations("Pa", 2);
    }

    @Test
    void creerActivite_refuseUneLocationInexistante() {
        CreerActiviteCommand command = new CreerActiviteCommand(
                1L, "Run", TypeSport.COURSE, LocalDate.now(), 5000.0, 30, "PasDeVille", 4, List.of());
        when(openMeteoService.locationExists("PasDeVille")).thenReturn(false);

        assertThatThrownBy(() -> service.creerActivite(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("introuvable");
    }

    @Test
    void recupererActivitesParUtilisateur_lanceSiUtilisateurAbsent() {
        when(utilisateurRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.recupererActivitesParUtilisateur(9L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Utilisateur introuvable");
    }

    @Test
    void recupererActivitesPourProfilEtParTypeSportEtDesAmis() {
        Activite activite = Activite.builder()
                .id(3L)
                .nom("Run")
                .typeSport(TypeSport.COURSE)
                .date(LocalDate.now())
                .utilisateur(utilisateur)
                .build();
        Activite activiteInvitee = Activite.builder()
                .id(4L)
                .nom("Ride")
                .typeSport(TypeSport.CYCLISME)
                .date(LocalDate.now().minusDays(1))
                .utilisateur(Utilisateur.builder().id(2L).prenom("Joe").nom("Friend").email("joe@test.com").motdepasse("x").build())
                .build();
        Utilisateur ami = Utilisateur.builder()
                .id(2L)
                .nom("Friend")
                .prenom("Joe")
                .email("joe@test.com")
                .motdepasse("x")
                .typeUtilisateur(TypeUtilisateur.UTILISATEUR)
                .build();
        utilisateur.setAmis(List.of(ami));
        when(activiteRepository.findByUtilisateurOuInvitesOrderByDateDesc(utilisateur)).thenReturn(List.of(activite, activiteInvitee));
        when(activiteRepository.findByTypeSport(TypeSport.COURSE)).thenReturn(List.of(activite));
        when(activiteRepository.findByUtilisateurIdsWithUtilisateurOrderByDateDesc(List.of(2L))).thenReturn(List.of(activite));

        assertThat(service.recupererActivitesPourProfil(utilisateur)).containsExactly(activite, activiteInvitee);
        assertThat(service.recupererActivitesParTypeSport(TypeSport.COURSE)).containsExactly(activite);
        assertThat(service.recupererActivitesDesAmis(utilisateur)).containsExactly(activite);
    }

    @Test
    void recupererActivitesFilActualite_inclutUtilisateurEtAmis() {
        Utilisateur ami = Utilisateur.builder()
                .id(2L)
                .nom("Friend")
                .prenom("Joe")
                .email("joe@test.com")
                .motdepasse("x")
                .typeUtilisateur(TypeUtilisateur.UTILISATEUR)
                .build();
        utilisateur.setAmis(List.of(ami));
        when(activiteRepository.findByUtilisateurIdsWithUtilisateurOrderByDateDesc(any())).thenReturn(List.of());

        service.recupererActivitesFilActualite(utilisateur);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.forClass((Class) List.class);
        verify(activiteRepository).findByUtilisateurIdsWithUtilisateurOrderByDateDesc(captor.capture());
        assertThat(captor.getValue()).containsExactly(1L, 2L);
    }

    @Test
    void recupererActivitesDesAmis_retourneVideQuandAucunAmiNestPresent() {
        utilisateur.setAmis(List.of());

        assertThat(service.recupererActivitesDesAmis(utilisateur)).isEmpty();
    }

    @Test
    void creerActivite_enregistreEtEnrichitActivite() {
        CreerActiviteCommand command = new CreerActiviteCommand(
                1L, "Run", TypeSport.COURSE, LocalDate.now(), 5000.0, 30, "Paris", 4, List.of(2L));
        Utilisateur invite = Utilisateur.builder()
                .id(2L)
                .nom("Friend")
                .prenom("Joe")
                .email("joe@test.com")
                .motdepasse("x")
                .typeUtilisateur(TypeUtilisateur.UTILISATEUR)
                .build();
        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(utilisateur));
        when(utilisateurRepository.findAllById(any())).thenReturn(List.of(invite));
        when(openMeteoService.locationExists("Paris")).thenReturn(true);
        when(openMeteoService.getWeatherForLocationAndDate("Paris", command.date()))
                .thenReturn(new OpenMeteoService.WeatherInfo(17.5, "Nuageux"));
        when(activiteRepository.save(any(Activite.class))).thenAnswer(invocation -> {
            Activite a = invocation.getArgument(0);
            a.setId(10L);
            return a;
        });

        Activite saved = service.creerActivite(command);

        assertThat(saved.getId()).isEqualTo(10L);
        assertThat(saved.getCalories()).isGreaterThan(0.0);
        assertThat(saved.getMeteoCondition()).isEqualTo("Nuageux");
        verify(utilisateurService).crediterExperience(utilisateur, saved.getXpGagne());
        verify(utilisateurService).crediterExperience(invite, saved.getXpGagne());
        verify(activiteBadgeEvaluationService).evaluerEtAttribuerBadges(saved);
    }

    @Test
    void creerActivite_refuseUneDateDansLeFutur() {
        CreerActiviteCommand command = new CreerActiviteCommand(
                1L, "Run", TypeSport.COURSE, LocalDate.now().plusDays(1), 5000.0, 30, "Paris", 4, List.of());

        assertThatThrownBy(() -> service.creerActivite(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("futur");
    }

    @Test
    void creerActiviteVersionSimple_enregistreEtCrediteExperience() {
        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(utilisateur));
        when(activiteRepository.save(any(Activite.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Activite saved = service.creerActivite(1L, "Run", TypeSport.COURSE, LocalDate.now());

        assertThat(saved.getNom()).isEqualTo("Run");
        verify(utilisateurService).crediterExperience(utilisateur, saved.getXpGagne());
        verify(activiteBadgeEvaluationService).evaluerEtAttribuerBadges(saved);
    }

    @Test
    void modifierActivite_metAJourEtRecalcule() {
        Activite activite = Activite.builder()
                .id(4L)
                .nom("Old")
                .typeSport(TypeSport.COURSE)
                .date(LocalDate.now().minusDays(1))
                .temps(20)
                .distance(3000.0)
                .xpGagne(42)
                .utilisateur(utilisateur)
                .invites(new ArrayList<>())
                .build();
        Utilisateur ami = Utilisateur.builder()
                .id(2L)
                .nom("Friend")
                .prenom("Joe")
                .email("joe@test.com")
                .motdepasse("x")
                .typeUtilisateur(TypeUtilisateur.UTILISATEUR)
                .build();
        when(activiteRepository.findByIdAvecUtilisateurEtInvites(4L)).thenReturn(Optional.of(activite));
        when(utilisateurRepository.findAllById(any())).thenReturn(List.of(ami));
        when(openMeteoService.locationExists("Lyon")).thenReturn(true);
        when(openMeteoService.getWeatherForLocationAndDate("Lyon", LocalDate.now()))
                .thenReturn(new OpenMeteoService.WeatherInfo(20.0, "Ensoleillé"));
        when(activiteRepository.save(activite)).thenReturn(activite);

        Activite updated = service.modifierActivite(
                4L,
                new ModifierActiviteRequest("New", TypeSport.CYCLISME, 10000.0, 60, LocalDate.now(), "Lyon", 5, List.of(2L)));

        assertThat(updated.getNom()).isEqualTo("New");
        assertThat(updated.getInvites()).containsExactly(ami);
        assertThat(updated.getMeteoCondition()).isEqualTo("Ensoleillé");
        verify(utilisateurService).crediterExperience(ami, 42);
    }

    @Test
    void modifierActivite_lanceUneErreurQuandLActiviteNExistePas() {
        when(activiteRepository.findByIdAvecUtilisateurEtInvites(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.modifierActivite(99L, new ModifierActiviteRequest(
                "New", TypeSport.CYCLISME, 10000.0, 60, LocalDate.now(), "Lyon", 5, List.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Activite introuvable");
    }

    @Test
    void supprimerEtCalculsFonctionnent() {
        Activite activite = Activite.builder()
                .id(4L)
                .typeSport(TypeSport.COURSE)
                .temps(60)
                .utilisateur(utilisateur)
                .date(LocalDate.now())
                .build();
        when(activiteRepository.existsById(4L)).thenReturn(true);
        when(activiteRepository.findById(4L)).thenReturn(Optional.of(activite));

        service.supprimerActivite(4L);

        verify(activiteRepository).deleteById(4L);
        assertThat(service.calculerKilocalories(4L)).isEqualTo(service.calculerKcalPourActivite(activite));
        assertThat(service.calculerKcalPourActivite(Activite.builder().typeSport(TypeSport.COURSE).utilisateur(utilisateur).date(LocalDate.now()).build()))
                .isEqualTo(0.0);
    }

    @Test
    void supprimerActivite_lanceUneErreurQuandLActiviteNexistePas() {
        when(activiteRepository.existsById(4L)).thenReturn(false);

        assertThatThrownBy(() -> service.supprimerActivite(4L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Activite introuvable");
    }

    @Test
    void recalculerMeteoEtCaloriesPourToutesLesActivites_metAJourSeulementLesManquantes() {
        Activite a = Activite.builder()
                .id(1L)
                .typeSport(TypeSport.COURSE)
                .temps(40)
                .utilisateur(utilisateur)
                .date(LocalDate.now())
                .location("Paris")
                .build();
        when(activiteRepository.findAll()).thenReturn(List.of(a));
        when(openMeteoService.getWeatherForLocationAndDate("Paris", a.getDate()))
                .thenReturn(new OpenMeteoService.WeatherInfo(17.0, "Nuageux"));

        int count = service.recalculerMeteoEtCaloriesPourToutesLesActivites();

        assertThat(count).isEqualTo(1);
        verify(activiteRepository).save(a);
    }

    @Test
    void recalculerMeteoEtCaloriesPourToutesLesActivites_neSauvegardeRienQuandToutEstDejaPresent() {
        Activite a = Activite.builder()
                .id(1L)
                .typeSport(TypeSport.COURSE)
                .temps(40)
                .utilisateur(utilisateur)
                .date(LocalDate.now())
                .location("Paris")
                .calories(123.0)
                .meteoCondition("Nuageux")
                .build();
        when(activiteRepository.findAll()).thenReturn(List.of(a));

        int count = service.recalculerMeteoEtCaloriesPourToutesLesActivites();

        assertThat(count).isEqualTo(0);
        org.mockito.Mockito.verify(activiteRepository, org.mockito.Mockito.never()).save(any(Activite.class));
    }
}
