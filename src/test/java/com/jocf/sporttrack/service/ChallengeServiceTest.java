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
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChallengeServiceTest {

    @Mock
    private ChallengeRepository challengeRepository;

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private ChallengeSaisieQuotidienneRepository challengeSaisieQuotidienneRepository;

    @Mock
    private UtilisateurService utilisateurService;

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
        service.setSelf(service);
        lenient().doAnswer(invocation -> {
            Long userId = invocation.getArgument(0);
            Integer points = invocation.getArgument(1);
            if (utilisateur.getId() != null && utilisateur.getId().equals(userId)) {
                utilisateur.soustraireHp(points);
            }
            return null;
        }).when(utilisateurService).appliquerPunitionChallenge(anyLong(), anyInt());
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
        when(challengeRepository.save(any(Challenge.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreerChallengeRequest request = new CreerChallengeRequest("Défi", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 3));
        Challenge saved = service.creerChallenge(request, 1L);

        assertThat(saved.getOrganisateur()).isSameAs(utilisateur);
        CreerChallengeRequest invalidRequest = new CreerChallengeRequest("Bad", LocalDate.of(2026, 1, 3), LocalDate.of(2026, 1, 1));
        assertThatThrownBy(() -> service.creerChallenge(invalidRequest, 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void creerChallenge_lanceUneErreurQuandOrganisateurAbsent() {
        when(utilisateurRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.creerChallenge(
                new CreerChallengeRequest("Défi", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 3)),
                1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Organisateur introuvable");
    }


    @Test
    void creerChallenge_DateFinNull_Couverture() {
        // GIVEN
        /* On crée une requête sans date de fin */
        CreerChallengeRequest req = new CreerChallengeRequest("Titre",  LocalDate.parse("2026-01-01"), null);

        // On utilise l'utilisateur de ton setUp
        /* Simulation du repository qui retourne l'utilisateur préparé dans le setUp */
        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(utilisateur));
        when(challengeRepository.save(any(Challenge.class))).thenAnswer(i -> i.getArguments()[0]);

        // WHEN
        Challenge result = service.creerChallenge(req, 1L);

        // THEN
        /* La date de fin doit être nulle */
        assertNull(result.getDateFin());
    }


    @Test
    void creerChallenge_DateFinInvalide_LanceException() {
        // GIVEN
        /* Date de fin (2025) avant la date de début (2026) */
        CreerChallengeRequest req = new CreerChallengeRequest("Titre",  LocalDate.parse("2026-01-01"), LocalDate.parse("2025-12-31"));

        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(utilisateur));

        // WHEN & THEN
        /* Vérifier que l'exception est bien déclenchée avec le bon message */
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.creerChallenge(req, 1L));

        assertEquals("La date de fin doit être après la date de début.", ex.getMessage());
    }

    @Test
    void creerChallenge_DateFinValide_Couverture() {

        CreerChallengeRequest req = new CreerChallengeRequest("Titre",  LocalDate.parse("2026-01-01"), LocalDate.parse("2026-02-01"));

        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(utilisateur));
        when(challengeRepository.save(any(Challenge.class))).thenAnswer(i -> i.getArguments()[0]);

        // WHEN
        Challenge result = service.creerChallenge(req, 1L);

        assertNotNull(result.getDateFin());
        assertTrue(result.getDateFin().after(result.getDateDebut()));
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
    void modifierChallenge_AvecDates_Couverture() {
        // GIVEN
        /* On prépare une requête avec des dates non nulles */
        ModifierChallengeRequest req = new ModifierChallengeRequest(
                "Nouveau Nom",
                java.time.LocalDate.parse("2026-05-01"), // dateDebut != null
                java.time.LocalDate.parse("2026-06-01") // dateFin != null

        );

        /* On utilise le challenge initialisé dans le setUp */
        when(challengeRepository.findById(2L)).thenReturn(Optional.of(challenge));
        when(challengeRepository.save(any(Challenge.class))).thenAnswer(i -> i.getArguments()[0]);

        // WHEN
        Challenge result = service.modifierChallenge(2L, req);

        assertEquals("Nouveau Nom", result.getNom());
        assertEquals(java.sql.Date.valueOf("2026-05-01"), result.getDateDebut());
        assertEquals(java.sql.Date.valueOf("2026-06-01"), result.getDateFin());
    }

    @Test
    void modifierChallenge_SansDates_Couverture() {
        // GIVEN
        /* On ne change que le nom, les dates sont nulles dans le DTO */
        ModifierChallengeRequest req = new ModifierChallengeRequest(
                "Nom Modifié",
                null, // dateDebut == null
                null// dateFin == null
        );

        /* Sauvegarder les dates originales du setUp pour comparer */
        java.sql.Date dateDebutOriginale = challenge.getDateDebut();
        java.sql.Date dateFinOriginale = challenge.getDateFin();

        when(challengeRepository.findById(2L)).thenReturn(Optional.of(challenge));
        when(challengeRepository.save(any(Challenge.class))).thenAnswer(i -> i.getArguments()[0]);

        // WHEN
        Challenge result = service.modifierChallenge(2L, req);

        assertEquals("Nom Modifié", result.getNom());
        assertEquals(dateDebutOriginale, result.getDateDebut());
        assertEquals(dateFinOriginale, result.getDateFin());
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
    void enregistrerSaisieQuotidienne_rejetteQuandLeParticipantNestPasInscrit() {
        Challenge challengeVide = Challenge.builder()
                .id(2L)
                .dateFin(java.sql.Date.valueOf(java.time.LocalDate.now().plusDays(10)))
                .participants(new java.util.HashSet<>())
                .build();
        when(challengeRepository.findById(2L)).thenReturn(Optional.of(challengeVide));
        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(utilisateur));

        assertThatThrownBy(() -> service.enregistrerSaisieQuotidienne(2L, 1L, LocalDate.now(), true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Vous ne participez pas");
    }

    @Test
    void enregistrerSaisieQuotidienne_sanctionneUnNonManuelUneSeuleFois() {
        when(challengeRepository.findById(2L)).thenReturn(Optional.of(challenge));
        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(utilisateur));
        when(challengeSaisieQuotidienneRepository.existsByChallenge_IdAndUtilisateur_IdAndJour(2L, 1L, LocalDate.now()))
                .thenReturn(false)
                .thenReturn(true);

        service.enregistrerSaisieQuotidienne(2L, 1L, LocalDate.now(), false);

        assertThat(utilisateur.getHpNormalise()).isEqualTo(99);
        verify(challengeSaisieQuotidienneRepository).save(any(ChallengeSaisieQuotidienne.class));

        Runnable duplicateSubmission = () -> service.enregistrerSaisieQuotidienne(2L, 1L, LocalDate.now(), false);
        assertThatThrownBy(duplicateSubmission::run)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Vous avez déjà répondu pour ce jour.");
    }

    @Test
    void enregistrerAbsenceQuotidienne_sanctionneDeuxPointsDeVie() {
        when(challengeRepository.findById(2L)).thenReturn(Optional.of(challenge));
        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(utilisateur));
        when(challengeSaisieQuotidienneRepository.existsByChallenge_IdAndUtilisateur_IdAndJour(2L, 1L, LocalDate.now()))
                .thenReturn(false);

        service.enregistrerAbsenceQuotidienne(2L, 1L, LocalDate.now());

        assertThat(utilisateur.getHpNormalise()).isEqualTo(98);
        verify(challengeSaisieQuotidienneRepository).save(any(ChallengeSaisieQuotidienne.class));
    }

    @Test
    void sanctionnerAbsencesQuotidiennes_ignoreLesChallengesInactifsEtPunitionAutomatique() {
        LocalDate hier = LocalDate.now().minusDays(1);
        Challenge challengeInactif = Challenge.builder()
                .id(3L)
                .dateDebut(Date.valueOf(LocalDate.now().plusDays(1)))
                .dateFin(Date.valueOf(LocalDate.now().plusDays(2)))
                .participants(Set.of(utilisateur))
                .build();
        when(challengeRepository.findAll()).thenReturn(List.of(challenge, challengeInactif));
        when(challengeSaisieQuotidienneRepository.existsByChallenge_IdAndUtilisateur_IdAndJour(2L, 1L, hier))
                .thenReturn(false)
                .thenReturn(false);
        when(challengeRepository.findById(2L)).thenReturn(Optional.of(challenge));
        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(utilisateur));

        service.sanctionnerAbsencesQuotidiennes(hier);

        assertThat(utilisateur.getHpNormalise()).isEqualTo(98);
        verify(challengeSaisieQuotidienneRepository).save(any(ChallengeSaisieQuotidienne.class));
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

    @Test
    void supprimerChallenge_Echec_SiInexistant() {
        // GIVEN
        Long idInexistant = 999L;
        /* Simuler que le challenge n'est pas présent dans la base de données */
        when(challengeRepository.existsById(idInexistant)).thenReturn(false);

                assertThrows(IllegalArgumentException.class,
                () -> service.supprimerChallenge(idInexistant));

        /* Vérifier que la suppression n'est jamais tentée si l'ID est invalide */
        verify(challengeRepository, never()).deleteById(anyLong());
    }

    /**
     * Cas 2 : Le challenge existe (Suppression réussie).
     */
    @Test
    void supprimerChallenge_Succes() {

        when(challengeRepository.existsById(2L)).thenReturn(true);

        service.supprimerChallenge(2L);

        verify(challengeRepository).deleteById(2L);
    }

    @Test
    void rejoindreChallenge_Echec_ChallengeTermine() {
        // GIVEN
        /* On modifie la date de fin pour qu'elle soit dans le passé */
        challenge.setDateFin(java.sql.Date.valueOf(java.time.LocalDate.now().minusDays(1)));

        when(challengeRepository.findById(2L)).thenReturn(Optional.of(challenge));
        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(utilisateur));

        // WHEN & THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.rejoindreChallenge(2L, 1L));

        assertEquals("Ce challenge est terminé.", ex.getMessage());
    }

    @Test
    void rejoindreChallenge_Echec_DejaParticipant() {
        // GIVEN
        /* Le challenge du setUp contient déjà l'utilisateur ID 1L */
        challenge.setDateFin(java.sql.Date.valueOf(java.time.LocalDate.now().plusDays(5)));

        when(challengeRepository.findById(2L)).thenReturn(Optional.of(challenge));
        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(utilisateur));

        // WHEN & THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.rejoindreChallenge(2L, 1L));

        assertEquals("Vous participez déjà à ce challenge.", ex.getMessage());
    }

    @Test
    void rejoindreChallenge_Succes() {
        Challenge challengeVide = Challenge.builder()
                .id(2L)
                .dateFin(java.sql.Date.valueOf(java.time.LocalDate.now().plusDays(10)))
                .participants(new java.util.HashSet<>()) // Liste vide
                .build();

        when(challengeRepository.findById(2L)).thenReturn(Optional.of(challengeVide));
        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(utilisateur));
        when(challengeRepository.save(any(Challenge.class))).thenAnswer(i -> i.getArguments()[0]);

        // WHEN
        service.rejoindreChallenge(2L, 1L);

        verify(challengeRepository).save(any(Challenge.class));
    }

    @Test
    void rejoindreChallenge_DateFinNull_Couverture() {
        Challenge challengeSansFin = Challenge.builder()
                .id(2L)
                .dateFin(null) // Condition A : dateFin != null sera FALSE
                .participants(new java.util.HashSet<>()) // Liste vide pour passer le 2ème IF
                .build();

        when(challengeRepository.findById(2L)).thenReturn(Optional.of(challengeSansFin));
        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(utilisateur));
        when(challengeRepository.save(any(Challenge.class))).thenAnswer(i -> i.getArguments()[0]);

        service.rejoindreChallenge(2L, 1L);

        verify(challengeRepository).save(any(Challenge.class));
    }

    @Test
    void rejoindreChallenge_Echec_HpNul() {
        Utilisateur utilisateurMort = Utilisateur.builder()
                .id(1L)
                .nom("Doe")
                .prenom("Jane")
                .email("jane@test.com")
                .motdepasse("x")
                .typeUtilisateur(TypeUtilisateur.UTILISATEUR)
                .hp(0)
                .build();

        Challenge challengeVide = Challenge.builder()
                .id(2L)
                .dateFin(java.sql.Date.valueOf(java.time.LocalDate.now().plusDays(10)))
                .participants(new java.util.HashSet<>())
                .build();

        when(challengeRepository.findById(2L)).thenReturn(Optional.of(challengeVide));
        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(utilisateurMort));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.rejoindreChallenge(2L, 1L));

        assertEquals("Action impossible : votre barre de vie est à 0.", ex.getMessage());
        verify(challengeRepository, never()).save(any());
    }

    @Test
    void supprimerChallengeSiOrganisateur_Echec_OrganisateurNull() {
        // GIVEN
        Long challengeId = 2L;
        Long userId = 1L;

        /* On crée un challenge qui n'a pas d'organisateur (cas rare mais possible en BDD) */
        Challenge challengeSansOrg = Challenge.builder().id(challengeId).organisateur(null).build();

        when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challengeSansOrg));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.supprimerChallengeSiOrganisateur(challengeId, userId));

        assertEquals("Seul l'organisateur peut supprimer ce challenge.", ex.getMessage());
    }

    @Test
    void supprimerChallengeSiOrganisateur_Echec_NonOrganisateur() {
        // GIVEN
        Long challengeId = 2L;
        Long pirateId = 1L; // Moi
        Long vraiOrgId = 99L; // Le vrai chef

        Utilisateur vraiChef = Utilisateur.builder().id(vraiOrgId).build();
        Challenge challengeAutre = Challenge.builder().id(challengeId).organisateur(vraiChef).build();

        when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challengeAutre));

        assertThrows(IllegalArgumentException.class,
                () -> service.supprimerChallengeSiOrganisateur(challengeId, pirateId));
    }

    @Test
    void supprimerChallengeSiOrganisateur_Succes() {
        // GIVEN
        Long challengeId = 2L;
        Long orgId = 1L;

        Utilisateur organisateur = Utilisateur.builder().id(orgId).build();
        Challenge monChallenge = Challenge.builder().id(challengeId).organisateur(organisateur).build();

        when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(monChallenge));

        service.supprimerChallengeSiOrganisateur(challengeId, orgId);

        verify(challengeSaisieQuotidienneRepository).deleteByChallenge(monChallenge);
        verify(challengeRepository).delete(monChallenge);
    }
}
