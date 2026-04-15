package com.jocf.sporttrack.service;

import com.jocf.sporttrack.dto.CreerChallengeRequest;
import com.jocf.sporttrack.dto.LigneClassementChallenge;
import com.jocf.sporttrack.dto.ModifierChallengeRequest;
import com.jocf.sporttrack.model.Challenge;
import com.jocf.sporttrack.model.ChallengeSaisieQuotidienne;
import com.jocf.sporttrack.enumeration.TypeUtilisateur;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.repository.ChallengeRepository;
import com.jocf.sporttrack.repository.ChallengeSaisieQuotidienneRepository;
import com.jocf.sporttrack.repository.UtilisateurRepository;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChallengeServiceTest {

    @Mock
    private ChallengeRepository challengeRepository;

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private ChallengeSaisieQuotidienneRepository challengeSaisieQuotidienneRepository;

    @InjectMocks
    private ChallengeService service;

    private Utilisateur utilisateur;
    private Challenge challenge;

    @BeforeEach
    void setUp() {
        utilisateur = Utilisateur.builder()
                .id(1L)
                .nom("Doe")
                .prenom("Jane")
                .email("jane@test.com")
                .motdepasse("x")
                .typeUtilisateur(TypeUtilisateur.UTILISATEUR)
                .build();
        challenge = Challenge.builder()
                .id(2L)
                .nom("Défi")
                .dateDebut(Date.valueOf(LocalDate.now().minusDays(2)))
                .dateFin(Date.valueOf(LocalDate.now().plusDays(2)))
                .organisateur(utilisateur)
                .participants(Set.of(utilisateur))
                .build();
    }

    @Test
    void recupererTousEtTrouverParId_deleguent() {
        when(challengeRepository.findAll()).thenReturn(List.of(challenge));
        when(challengeRepository.findById(2L)).thenReturn(Optional.of(challenge));

        assertThat(service.recupererTousLesChallenges()).containsExactly(challenge);
        assertThat(service.trouverParId(2L)).contains(challenge);
    }

    @Test
    void creerChallenge_valideDatesEtSauvegarde() {
        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(utilisateur));
        when(challengeRepository.save(org.mockito.ArgumentMatchers.any(Challenge.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Challenge saved = service.creerChallenge(
                new CreerChallengeRequest("Défi", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 3)),
                1L);

        assertThat(saved.getOrganisateur()).isSameAs(utilisateur);
        assertThatThrownBy(() -> service.creerChallenge(
                new CreerChallengeRequest("Bad", LocalDate.of(2026, 1, 3), LocalDate.of(2026, 1, 1)), 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void modifierEtSupprimerChallenge_fonctionnent() {
        when(challengeRepository.findById(2L)).thenReturn(Optional.of(challenge));
        when(challengeRepository.save(challenge)).thenReturn(challenge);
        when(challengeRepository.existsById(2L)).thenReturn(true);

        Challenge updated = service.modifierChallenge(2L, new ModifierChallengeRequest("Nouveau", LocalDate.now(), LocalDate.now().plusDays(1)));
        service.supprimerChallenge(2L);

        assertThat(updated.getNom()).isEqualTo("Nouveau");
        verify(challengeRepository).deleteById(2L);
    }

    @Test
    void getClassement_agregeLesScores() {
        Utilisateur autre = Utilisateur.builder()
                .id(3L).nom("B").prenom("B").email("b@test.com").motdepasse("x").typeUtilisateur(TypeUtilisateur.UTILISATEUR).build();
        challenge.setParticipants(Set.of(utilisateur, autre));
        when(challengeRepository.findById(2L)).thenReturn(Optional.of(challenge));
        when(challengeSaisieQuotidienneRepository.findByChallengeAndRealiseTrue(challenge)).thenReturn(List.of(
                ChallengeSaisieQuotidienne.builder().challenge(challenge).utilisateur(utilisateur).jour(LocalDate.now()).realise(true).build(),
                ChallengeSaisieQuotidienne.builder().challenge(challenge).utilisateur(utilisateur).jour(LocalDate.now().minusDays(1)).realise(true).build()
        ));

        List<LigneClassementChallenge> classement = service.getClassement(2L);

        assertThat(classement.get(0).getUtilisateur()).isEqualTo(utilisateur);
        assertThat(classement.get(0).getScore()).isEqualTo(2L);
    }

    @Test
    void rejoindreEtRecupererReponseDuJour_fonctionnent() {
        Utilisateur autre = Utilisateur.builder()
                .id(3L).nom("B").prenom("B").email("b@test.com").motdepasse("x").typeUtilisateur(TypeUtilisateur.UTILISATEUR).build();
        challenge.setParticipants(new java.util.HashSet<>());
        when(challengeRepository.findById(2L)).thenReturn(Optional.of(challenge));
        when(utilisateurRepository.findById(3L)).thenReturn(Optional.of(autre));
        when(challengeRepository.save(challenge)).thenReturn(challenge);
        when(challengeSaisieQuotidienneRepository.findByChallengeAndUtilisateurAndJour(challenge, autre, LocalDate.now()))
                .thenReturn(Optional.of(ChallengeSaisieQuotidienne.builder().realise(true).build()));

        Challenge joined = service.rejoindreChallenge(2L, 3L);
        Boolean realise = service.recupererReponseDuJour(2L, 3L, LocalDate.now());

        assertThat(joined.getParticipants()).contains(autre);
        assertThat(realise).isTrue();
    }

    @Test
    void enregistrerSaisieQuotidienne_creePuisMetAJour() {
        when(challengeRepository.findById(2L)).thenReturn(Optional.of(challenge));
        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(utilisateur));
        when(challengeSaisieQuotidienneRepository.findByChallengeAndUtilisateurAndJour(challenge, utilisateur, LocalDate.now()))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(ChallengeSaisieQuotidienne.builder()
                        .challenge(challenge).utilisateur(utilisateur).jour(LocalDate.now()).realise(false).build()));

        service.enregistrerSaisieQuotidienne(2L, 1L, LocalDate.now(), true);
        service.enregistrerSaisieQuotidienne(2L, 1L, LocalDate.now(), false);

        verify(challengeSaisieQuotidienneRepository, org.mockito.Mockito.times(2))
                .save(org.mockito.ArgumentMatchers.any(ChallengeSaisieQuotidienne.class));
    }

    @Test
    void supprimerChallengeSiOrganisateurEtTrouverChallengesTerminesLe() {
        when(challengeRepository.findById(2L)).thenReturn(Optional.of(challenge));
        when(challengeRepository.findByDateFin(Date.valueOf(LocalDate.of(2026, 4, 1)))).thenReturn(List.of(challenge));

        service.supprimerChallengeSiOrganisateur(2L, 1L);

        verify(challengeSaisieQuotidienneRepository).deleteByChallenge(challenge);
        verify(challengeRepository).delete(challenge);
        assertThat(service.trouverChallengesTerminesLe(LocalDate.of(2026, 4, 1))).containsExactly(challenge);
    }
}
