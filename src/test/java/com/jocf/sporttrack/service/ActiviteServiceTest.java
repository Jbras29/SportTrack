package com.jocf.sporttrack.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jocf.sporttrack.model.Activite;
import com.jocf.sporttrack.model.TypeSport;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.repository.ActiviteRepository;
import com.jocf.sporttrack.repository.UtilisateurRepository;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ActiviteServiceTest {

    @Mock
    private ActiviteRepository activiteRepository;

    @Mock
    private UtilisateurRepository utilisateurRepository;

    private ActiviteService activiteService;

    @BeforeEach
    void setUp() {
        activiteService = new ActiviteService(activiteRepository, utilisateurRepository);
    }

    @Test
    void recupererToutesLesActivitesRetourneLaListe() {
        Activite activite1 = Activite.builder().id(1L).nom("Course matinale").build();
        Activite activite2 = Activite.builder().id(2L).nom("Yoga").build();
        List<Activite> activites = Arrays.asList(activite1, activite2);

        when(activiteRepository.findAll()).thenReturn(activites);

        List<Activite> resultat = activiteService.recupererToutesLesActivites();

        assertEquals(2, resultat.size());
        assertEquals("Course matinale", resultat.get(0).getNom());
        assertEquals("Yoga", resultat.get(1).getNom());
        verify(activiteRepository).findAll();
    }

    @Test
    void trouverParIdRetourneLActiviteSiPresente() {
        Activite activite = Activite.builder().id(1L).nom("Test Activite").build();

        when(activiteRepository.findById(1L)).thenReturn(Optional.of(activite));

        Optional<Activite> resultat = activiteService.trouverParId(1L);

        assertTrue(resultat.isPresent());
        assertEquals("Test Activite", resultat.get().getNom());
        verify(activiteRepository).findById(1L);
    }

    @Test
    void recupererActivitesParUtilisateurRetourneLaListe() {
        Utilisateur utilisateur = Utilisateur.builder().id(1L).build();
        Activite activite1 = Activite.builder().id(1L).nom("Activite 1").build();
        Activite activite2 = Activite.builder().id(2L).nom("Activite 2").build();
        List<Activite> activites = Arrays.asList(activite1, activite2);

        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(utilisateur));
        when(activiteRepository.findByUtilisateur(utilisateur)).thenReturn(activites);

        List<Activite> resultat = activiteService.recupererActivitesParUtilisateur(1L);

        assertEquals(2, resultat.size());
        verify(utilisateurRepository).findById(1L);
        verify(activiteRepository).findByUtilisateur(utilisateur);
    }

    @Test
    void recupererActivitesParUtilisateurRefuseUtilisateurInexistant() {
        when(utilisateurRepository.findById(1L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> activiteService.recupererActivitesParUtilisateur(1L));

        assertEquals("Utilisateur introuvable : 1", exception.getMessage());
        verify(utilisateurRepository).findById(1L);
    }

    @Test
    void recupererActivitesParTypeSportRetourneLaListe() {
        Activite activite1 = Activite.builder().id(1L).nom("Course").typeSport(TypeSport.COURSE).build();
        Activite activite2 = Activite.builder().id(2L).nom("Marche").typeSport(TypeSport.COURSE).build();
        List<Activite> activites = Arrays.asList(activite1, activite2);

        when(activiteRepository.findByTypeSport(TypeSport.COURSE)).thenReturn(activites);

        List<Activite> resultat = activiteService.recupererActivitesParTypeSport(TypeSport.COURSE);

        assertEquals(2, resultat.size());
        verify(activiteRepository).findByTypeSport(TypeSport.COURSE);
    }

    @Test
    void creerActiviteAvecUtilisateurValide() {
        Utilisateur utilisateur = Utilisateur.builder().id(1L).email("user").build();
        LocalDate date = LocalDate.of(2024, 4, 7);

        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(utilisateur));
        when(activiteRepository.save(any(Activite.class))).thenAnswer(invocation -> {
            Activite saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        Activite resultat = activiteService.creerActivite(1L, "Nouvelle Activite", TypeSport.COURSE, date);

        assertNotNull(resultat.getId());
        assertEquals("Nouvelle Activite", resultat.getNom());
        assertEquals(TypeSport.COURSE, resultat.getTypeSport());
        assertEquals(date, resultat.getDate());
        assertEquals(utilisateur, resultat.getUtilisateur());
        verify(utilisateurRepository).findById(1L);
        verify(activiteRepository).save(any(Activite.class));
    }

    @Test
    void creerActiviteRefuseUtilisateurInexistant() {
        LocalDate date = LocalDate.of(2024, 4, 7);

        when(utilisateurRepository.findById(1L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> activiteService.creerActivite(1L, "Activite", TypeSport.COURSE, date));

        assertEquals("Utilisateur introuvable : 1", exception.getMessage());
        verify(utilisateurRepository).findById(1L);
    }

    @Test
    void modifierActiviteMetAJourLesDetails() {
        Activite existante = Activite.builder()
                .id(1L)
                .nom("Ancien Nom")
                .typeSport(TypeSport.COURSE)
                .build();
        Activite updates = Activite.builder()
                .nom("Nouveau Nom")
                .typeSport(TypeSport.YOGA)
                .distance(10.5)
                .temps(60)
                .date(LocalDate.of(2024, 4, 8))
                .location("Parc")
                .evaluation(5)
                .build();

        when(activiteRepository.findById(1L)).thenReturn(Optional.of(existante));
        when(activiteRepository.save(any(Activite.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Activite resultat = activiteService.modifierActivite(1L, updates);

        assertEquals("Nouveau Nom", resultat.getNom());
        assertEquals(TypeSport.YOGA, resultat.getTypeSport());
        assertEquals(10.5, resultat.getDistance());
        assertEquals(60, resultat.getTemps());
        assertEquals(LocalDate.of(2024, 4, 8), resultat.getDate());
        assertEquals("Parc", resultat.getLocation());
        assertEquals(5, resultat.getEvaluation());
        verify(activiteRepository).findById(1L);
        verify(activiteRepository).save(existante);
    }

    @Test
    void modifierActiviteRefuseIdInexistant() {
        Activite updates = Activite.builder().nom("Update").build();

        when(activiteRepository.findById(1L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> activiteService.modifierActivite(1L, updates));

        assertEquals("Activite introuvable : 1", exception.getMessage());
        verify(activiteRepository).findById(1L);
    }

    @Test
    void supprimerActiviteSupprimeSiExiste() {
        when(activiteRepository.existsById(1L)).thenReturn(true);

        activiteService.supprimerActivite(1L);

        verify(activiteRepository).existsById(1L);
        verify(activiteRepository).deleteById(1L);
    }

    @Test
    void supprimerActiviteRefuseIdInexistant() {
        when(activiteRepository.existsById(1L)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> activiteService.supprimerActivite(1L));

        assertEquals("Activite introuvable : 1", exception.getMessage());
        verify(activiteRepository).existsById(1L);
    }
}