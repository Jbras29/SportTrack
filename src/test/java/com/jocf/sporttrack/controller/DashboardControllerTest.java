package com.jocf.sporttrack.controller;

import com.jocf.sporttrack.enumeration.TypeSport;
import com.jocf.sporttrack.enumeration.TypeUtilisateur;
import com.jocf.sporttrack.model.Activite;
import com.jocf.sporttrack.model.Challenge;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.repository.ActiviteRepository;
import com.jocf.sporttrack.repository.ChallengeRepository;
import com.jocf.sporttrack.service.UtilisateurService;
import java.time.LocalDate;
import java.util.Map;
import java.util.List;
import java.lang.reflect.Method;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock
    private UtilisateurService utilisateurService;

    @Mock
    private ActiviteRepository activiteRepository;

    @Mock
    private ChallengeRepository challengeRepository;

    @InjectMocks
    private DashboardController controller;

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
                .xp(120)
                .hp(80)
                .build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void dashboard_redirigeQuandPasAuthentifie() {
        String view = controller.dashboard(new ExtendedModelMap());

        assertThat(view).isEqualTo("redirect:/Utilisateur/");
    }

    @Test
    void dashboard_redirigeQuandAuthenticationNonValide() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("jane@test.com", "x"));

        String view = controller.dashboard(new ExtendedModelMap());

        assertThat(view).isEqualTo("redirect:/Utilisateur/");
    }

    @Test
    void dashboard_redirigeQuandLeServiceUtilisateurEchoue() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        "jane@test.com", "x", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        when(utilisateurService.trouverParEmail("jane@test.com"))
                .thenThrow(new RuntimeException("boom"));

        String view = controller.dashboard(new ExtendedModelMap());

        assertThat(view).isEqualTo("redirect:/Utilisateur/");
    }

    @Test
    void dashboard_gereLesEtatsVidesEtLesDefisSanstropDeDonnees() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        "jane@test.com", "x", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        when(utilisateurService.trouverParEmail("jane@test.com")).thenReturn(utilisateur);
        when(activiteRepository.findByUtilisateurOrderByDateDesc(utilisateur)).thenReturn(List.of());
        when(challengeRepository.findByParticipants_IdOrderByDateFinAsc(1L)).thenReturn(List.of());

        Model model = new ExtendedModelMap();

        String view = controller.dashboard(model);

        assertThat(view).isEqualTo("dashboard");
        assertThat(model.getAttribute("totalDistanceKm")).isEqualTo(0.0);
        assertThat(model.getAttribute("totalTempsMinutes")).isEqualTo(0);
        assertThat(model.getAttribute("totalTempsTexte")).isEqualTo("0min");
        assertThat(model.getAttribute("moyenneDistanceKm")).isEqualTo(0.0);
        assertThat(model.getAttribute("moyenneTempsTexte")).isEqualTo("0min");
        assertThat(model.getAttribute("allureMoyenne")).isEqualTo("—");
        assertThat(model.getAttribute("sportFavori")).isEqualTo("Aucun");
        assertThat(model.getAttribute("sportFavoriSeances")).isEqualTo(0L);
        assertThat(model.getAttribute("derniereSeanceTexte")).isEqualTo("Aucune séance");
        assertThat((List<?>) model.getAttribute("semaineActivite")).hasSize(7);
        assertThat((List<?>) model.getAttribute("defisEnCoursAffichage")).isEmpty();
    }

    @Test
    void dashboard_remplitModele() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        "jane@test.com", "x", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        Activite activite = Activite.builder()
                .date(LocalDate.now())
                .temps(30)
                .distance(42.0)
                .typeSport(TypeSport.COURSE)
                .utilisateur(utilisateur)
                .build();
        when(utilisateurService.trouverParEmail("jane@test.com")).thenReturn(utilisateur);
        when(activiteRepository.findByUtilisateurOrderByDateDesc(utilisateur))
                .thenReturn(List.of(activite));
        when(challengeRepository.findByParticipants_IdOrderByDateFinAsc(1L))
                .thenReturn(List.of());
        Model model = new ExtendedModelMap();

        String view = controller.dashboard(model);

        assertThat(view).isEqualTo("dashboard");
        assertThat(model.getAttribute("totalDistanceKm")).isEqualTo(42.0);
        assertThat(model.getAttribute("nombreActivites")).isEqualTo(1L);
        assertThat((List<?>) model.getAttribute("semaineActivite")).hasSize(7);
    }

    @Test
    void dashboard_afficheLesCartesDeDefisDansTousLesCas() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        "jane@test.com", "x", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        when(utilisateurService.trouverParEmail("jane@test.com")).thenReturn(utilisateur);
        when(activiteRepository.findByUtilisateurOrderByDateDesc(utilisateur)).thenReturn(List.of());

        LocalDate today = LocalDate.now();
        Challenge challengeEnCours = challenge(today.minusDays(2), today.plusDays(2));
        Challenge challengeDemain = challenge(today.plusDays(1), today.plusDays(3));
        Challenge challengePlusTard = challenge(today.plusDays(3), today.plusDays(6));
        Challenge challengeDebutSeul = Challenge.builder()
                .nom("Debut seul")
                .dateDebut(java.sql.Date.valueOf(today))
                .build();
        Challenge challengeFinSeule = Challenge.builder()
                .nom("Fin seule")
                .dateFin(java.sql.Date.valueOf(today.plusDays(1)))
                .build();
        Challenge challengeVide = Challenge.builder().nom("Vide").build();
        when(challengeRepository.findByParticipants_IdOrderByDateFinAsc(1L))
                .thenReturn(List.of(
                        challengeEnCours,
                        challengeDemain,
                        challengePlusTard,
                        challengeDebutSeul,
                        challengeFinSeule,
                        challengeVide));

        Model model = new ExtendedModelMap();

        String view = controller.dashboard(model);

        assertThat(view).isEqualTo("dashboard");
        List<Map<String, Object>> cartes = (List<Map<String, Object>>) model.getAttribute("defisEnCoursAffichage");
        assertThat(cartes).hasSize(6);
        assertThat(cartes.get(0)).containsEntry("statutTemps", "2 jours restants");
        assertThat(cartes.get(1)).containsEntry("statutTemps", "Commence demain");
        assertThat(cartes.get(2)).containsEntry("statutTemps", "Commence dans 3 jours");
        assertThat(cartes.get(3)).containsEntry("statutTemps", "Date de fin non définie");
        assertThat(cartes.get(4)).containsEntry("statutTemps", "1 jour restant");
        assertThat(cartes.get(5)).containsEntry("statutTemps", "Aucune date disponible");
    }

    @Test
    void formatDurationEtAllureEtRelativeDays_couvrentLesBranches() throws Exception {
        assertThat(invokeStaticString("formatDuration", new Class<?>[] {int.class}, 0)).isEqualTo("0min");
        assertThat(invokeStaticString("formatDuration", new Class<?>[] {int.class}, 30)).isEqualTo("30min");
        assertThat(invokeStaticString("formatDuration", new Class<?>[] {int.class}, 90)).isEqualTo("1h 30min");

        assertThat(invokeStaticString("formatAllure", new Class<?>[] {double.class}, 0.0)).isEqualTo("—");
        assertThat(invokeStaticString("formatAllure", new Class<?>[] {double.class}, 1.999)).isEqualTo("2'00\"/km");

        assertThat(invokeStaticString("formatRelativeDays", new Class<?>[] {long.class}, 0L)).isEqualTo("Aujourd'hui");
        assertThat(invokeStaticString("formatRelativeDays", new Class<?>[] {long.class}, 1L)).isEqualTo("1 jour");
        assertThat(invokeStaticString("formatRelativeDays", new Class<?>[] {long.class}, 3L)).isEqualTo("3 jours");
    }

    @Test
    void calculerProgression_etStatutTemps_couvrentLesCasLimites() throws Exception {
        LocalDate today = LocalDate.of(2026, 4, 16);

        Challenge challengeSansDates = Challenge.builder().build();
        assertThat(invokeStaticInt("calculerProgression", new Class<?>[] {Challenge.class, LocalDate.class}, challengeSansDates, today))
                .isZero();

        Challenge challengeFutur = challenge(today.plusDays(2), today.plusDays(5));
        assertThat(invokeStaticInt("calculerProgression", new Class<?>[] {Challenge.class, LocalDate.class}, challengeFutur, today))
                .isZero();

        Challenge challengeInvalide = challenge(today.minusDays(1), today.minusDays(2));
        assertThat(invokeStaticInt("calculerProgression", new Class<?>[] {Challenge.class, LocalDate.class}, challengeInvalide, today))
                .isEqualTo(100);

        assertThat(invokeStaticString("calculerStatutTemps",
                new Class<?>[] {LocalDate.class, LocalDate.class, LocalDate.class},
                null, null, today)).isEqualTo("Date de fin non définie");
        assertThat(invokeStaticString("calculerStatutTemps",
                new Class<?>[] {LocalDate.class, LocalDate.class, LocalDate.class},
                today.plusDays(1), today.plusDays(1), today)).isEqualTo("Commence demain");
        assertThat(invokeStaticString("calculerStatutTemps",
                new Class<?>[] {LocalDate.class, LocalDate.class, LocalDate.class},
                today.plusDays(3), today.plusDays(3), today)).isEqualTo("Commence dans 3 jours");
        assertThat(invokeStaticString("calculerStatutTemps",
                new Class<?>[] {LocalDate.class, LocalDate.class, LocalDate.class},
                today.minusDays(3), today.minusDays(1), today)).isEqualTo("Terminé");
        assertThat(invokeStaticString("calculerStatutTemps",
                new Class<?>[] {LocalDate.class, LocalDate.class, LocalDate.class},
                today.minusDays(1), today, today)).isEqualTo("Se termine aujourd'hui");
        assertThat(invokeStaticString("calculerStatutTemps",
                new Class<?>[] {LocalDate.class, LocalDate.class, LocalDate.class},
                today.minusDays(2), today.plusDays(1), today)).isEqualTo("1 jour restant");
        assertThat(invokeStaticString("calculerStatutTemps",
                new Class<?>[] {LocalDate.class, LocalDate.class, LocalDate.class},
                today.minusDays(2), today.plusDays(3), today)).isEqualTo("3 jours restants");
    }

    private static Challenge challenge(LocalDate debut, LocalDate fin) {
        return Challenge.builder()
                .nom("Defi")
                .dateDebut(java.sql.Date.valueOf(debut))
                .dateFin(java.sql.Date.valueOf(fin))
                .build();
    }

    private static String invokeStaticString(String name, Class<?>[] parameterTypes, Object... args) throws Exception {
        return (String) invokeStatic(name, parameterTypes, args);
    }

    private static int invokeStaticInt(String name, Class<?>[] parameterTypes, Object... args) throws Exception {
        return (Integer) invokeStatic(name, parameterTypes, args);
    }

    private static Object invokeStatic(String name, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = DashboardController.class.getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        return method.invoke(null, args);
    }
}
