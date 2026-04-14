package com.jocf.sporttrack.controller;

import com.jocf.sporttrack.model.Activite;
import com.jocf.sporttrack.model.Challenge;
import com.jocf.sporttrack.model.TypeSport;
import com.jocf.sporttrack.model.TypeUtilisateur;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.repository.ActiviteRepository;
import com.jocf.sporttrack.repository.ChallengeRepository;
import com.jocf.sporttrack.service.UtilisateurService;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
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
    void dashboard_remplitModele() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        "jane@test.com", "x", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        Activite activite = Activite.builder()
                .date(LocalDate.now())
                .temps(30)
                .typeSport(TypeSport.COURSE)
                .utilisateur(utilisateur)
                .build();
        Challenge challenge = Challenge.builder()
                .dateFin(Date.valueOf(LocalDate.now().plusDays(1)))
                .build();
        when(utilisateurService.trouverParEmail("jane@test.com")).thenReturn(utilisateur);
        when(activiteRepository.sumDistanceByUtilisateur(utilisateur)).thenReturn(42.0);
        when(activiteRepository.sumTempsMinutesByUtilisateur(utilisateur)).thenReturn(120);
        when(activiteRepository.countByUtilisateur(utilisateur)).thenReturn(3L);
        when(challengeRepository.findByParticipants_IdOrderByDateFinAsc(1L)).thenReturn(List.of(challenge));
        when(activiteRepository.findTop5ByUtilisateurOrderByDateDesc(utilisateur)).thenReturn(List.of(activite));
        when(activiteRepository.findByUtilisateurAndDateBetween(
                org.mockito.ArgumentMatchers.eq(utilisateur),
                org.mockito.ArgumentMatchers.any(LocalDate.class),
                org.mockito.ArgumentMatchers.any(LocalDate.class)))
                .thenReturn(List.of(activite));
        Model model = new ExtendedModelMap();

        String view = controller.dashboard(model);

        assertThat(view).isEqualTo("dashboard");
        assertThat(model.getAttribute("totalDistanceKm")).isEqualTo(42.0);
        assertThat(model.getAttribute("nombreActivites")).isEqualTo(3L);
        assertThat((List<?>) model.getAttribute("semaineActivite")).hasSize(7);
    }
}
