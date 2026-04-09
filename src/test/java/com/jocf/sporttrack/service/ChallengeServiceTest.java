package com.jocf.sporttrack.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jocf.sporttrack.model.Challenge;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.repository.ChallengeRepository;
import com.jocf.sporttrack.repository.UtilisateurRepository;
import java.sql.Date;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChallengeServiceTest {

    @Mock
    private ChallengeRepository challengeRepository;

    @Mock
    private UtilisateurRepository utilisateurRepository;

    private ChallengeService challengeService;

    @BeforeEach
    void setUp() {
        challengeService = new ChallengeService(challengeRepository, utilisateurRepository);
    }

    @Test
    void recupererTousLesChallengesRetourneLaListe() {
        Challenge challenge1 = Challenge.builder().id(1L).nom("Challenge 1").build();
        Challenge challenge2 = Challenge.builder().id(2L).nom("Challenge 2").build();
        List<Challenge> challenges = Arrays.asList(challenge1, challenge2);

        when(challengeRepository.findAll()).thenReturn(challenges);

        List<Challenge> resultat = challengeService.recupererTousLesChallenges();

        assertEquals(2, resultat.size());
        assertEquals("Challenge 1", resultat.get(0).getNom());
        assertEquals("Challenge 2", resultat.get(1).getNom());
        verify(challengeRepository).findAll();
    }

    @Test
    void trouverParIdRetourneLeChallengeSiPresent() {
        Challenge challenge = Challenge.builder().id(1L).nom("Test Challenge").build();

        when(challengeRepository.findById(1L)).thenReturn(Optional.of(challenge));

        Optional<Challenge> resultat = challengeService.trouverParId(1L);

        assertTrue(resultat.isPresent());
        assertEquals("Test Challenge", resultat.get().getNom());
        verify(challengeRepository).findById(1L);
    }

    @Test
    void trouverParIdRetourneVideSiAbsent() {
        when(challengeRepository.findById(1L)).thenReturn(Optional.empty());

        Optional<Challenge> resultat = challengeService.trouverParId(1L);

        assertTrue(resultat.isEmpty());
        verify(challengeRepository).findById(1L);
    }

    @Test
    void creerChallengeAvecOrganisateurValide() {
        Utilisateur organisateur = Utilisateur.builder().id(1L).email("organisateur").build();
        Challenge challenge = Challenge.builder()
                .nom("Nouveau Challenge")
                .dateDebut(Date.valueOf("2024-01-01"))
                .dateFin(Date.valueOf("2024-01-31"))
                .build();

        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(organisateur));
        when(challengeRepository.save(any(Challenge.class))).thenAnswer(invocation -> {
            Challenge saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        Challenge resultat = challengeService.creerChallenge(challenge, 1L);

        assertNotNull(resultat.getId());
        assertEquals("Nouveau Challenge", resultat.getNom());
        assertEquals(organisateur, resultat.getOrganisateur());
        verify(utilisateurRepository).findById(1L);
        verify(challengeRepository).save(any(Challenge.class));
    }

    @Test
    void creerChallengeRefuseOrganisateurInexistant() {
        Challenge challenge = Challenge.builder().nom("Challenge").build();

        when(utilisateurRepository.findById(1L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> challengeService.creerChallenge(challenge, 1L));

        assertEquals("Organisateur introuvable : 1", exception.getMessage());
        verify(utilisateurRepository).findById(1L);
        verify(challengeRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void modifierChallengeMetAJourLesDetails() {
        Challenge existing = Challenge.builder()
                .id(1L)
                .nom("Ancien Nom")
                .dateDebut(Date.valueOf("2024-01-01"))
                .dateFin(Date.valueOf("2024-01-31"))
                .build();
        Challenge updates = Challenge.builder()
                .nom("Nouveau Nom")
                .dateDebut(Date.valueOf("2024-02-01"))
                .dateFin(Date.valueOf("2024-02-28"))
                .build();

        when(challengeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(challengeRepository.save(any(Challenge.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Challenge resultat = challengeService.modifierChallenge(1L, updates);

        assertEquals("Nouveau Nom", resultat.getNom());
        assertEquals(Date.valueOf("2024-02-01"), resultat.getDateDebut());
        assertEquals(Date.valueOf("2024-02-28"), resultat.getDateFin());
        verify(challengeRepository).findById(1L);
        verify(challengeRepository).save(existing);
    }

    @Test
    void modifierChallengeRefuseIdInexistant() {
        Challenge updates = Challenge.builder().nom("Update").build();

        when(challengeRepository.findById(1L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> challengeService.modifierChallenge(1L, updates));

        assertEquals("Challenge introuvable : 1", exception.getMessage());
        verify(challengeRepository).findById(1L);
        verify(challengeRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void supprimerChallengeSupprimeSiExiste() {
        when(challengeRepository.existsById(1L)).thenReturn(true);

        challengeService.supprimerChallenge(1L);

        verify(challengeRepository).existsById(1L);
        verify(challengeRepository).deleteById(1L);
    }

    @Test
    void supprimerChallengeRefuseIdInexistant() {
        when(challengeRepository.existsById(1L)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> challengeService.supprimerChallenge(1L));

        assertEquals("Challenge introuvable : 1", exception.getMessage());
        verify(challengeRepository).existsById(1L);
        verify(challengeRepository, org.mockito.Mockito.never()).deleteById(1L);
    }

    @Test
void creerChallengeAvecDonneesValides() {
    Utilisateur organisateur = Utilisateur.builder().id(1L).build();
    Challenge challenge = Challenge.builder()
        .nom("Trail Challenge")
        .dateDebut(Date.valueOf("2026-05-01"))
        .dateFin(Date.valueOf("2026-05-31"))
        .build();

    when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(organisateur));
    when(challengeRepository.save(any(Challenge.class))).thenAnswer(invocation -> {
        Challenge saved = invocation.getArgument(0);
        return Challenge.builder()
            .id(1L)
            .nom(saved.getNom())
            .dateDebut(saved.getDateDebut())
            .dateFin(saved.getDateFin())
            .organisateur(saved.getOrganisateur())
            .build();
    });

    Challenge result = challengeService.creerChallenge(challenge, 1L);

    assertNotNull(result.getId());
    assertEquals("Trail Challenge", result.getNom());
    assertEquals(organisateur, result.getOrganisateur());
    verify(challengeRepository).save(any(Challenge.class));
}

@Test
void creerChallengeRefuseDateFinAvantDateDebut() {
    Utilisateur organisateur = Utilisateur.builder().id(1L).build();
    Challenge challenge = Challenge.builder()
        .nom("Trail Challenge")
        .dateDebut(Date.valueOf("2026-05-31"))
        .dateFin(Date.valueOf("2026-05-01"))
        .build();

    when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(organisateur));

    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> challengeService.creerChallenge(challenge, 1L)
    );

    assertEquals("La date de fin doit être après la date de début.", exception.getMessage());
}
}