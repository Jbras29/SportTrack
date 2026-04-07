package com.jocf.sporttrack.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jocf.sporttrack.model.PrefSportive;
import com.jocf.sporttrack.repository.PrefSportiveRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PrefSportiveServiceTest {

    @Mock
    private PrefSportiveRepository prefSportiveRepository;

    private PrefSportiveService prefSportiveService;

    @BeforeEach
    void setUp() {
        prefSportiveService = new PrefSportiveService(prefSportiveRepository);
    }

    @Test
    void recupererToutesLesPrefSportivesRetourneLaListe() {
        PrefSportive pref1 = PrefSportive.builder().id(1L).nom("Yoga").build();
        PrefSportive pref2 = PrefSportive.builder().id(2L).nom("Course").build();
        List<PrefSportive> preferences = Arrays.asList(pref1, pref2);

        when(prefSportiveRepository.findAll()).thenReturn(preferences);

        List<PrefSportive> resultat = prefSportiveService.recupererToutesLesPrefSportives();

        assertEquals(2, resultat.size());
        assertEquals("Yoga", resultat.get(0).getNom());
        assertEquals("Course", resultat.get(1).getNom());
        verify(prefSportiveRepository).findAll();
    }

    @Test
    void trouverParIdRetourneLaPreferenceSiPresente() {
        PrefSportive prefSportive = PrefSportive.builder().id(1L).nom("Yoga").build();

        when(prefSportiveRepository.findById(1L)).thenReturn(Optional.of(prefSportive));

        Optional<PrefSportive> resultat = prefSportiveService.trouverParId(1L);

        assertTrue(resultat.isPresent());
        assertEquals("Yoga", resultat.get().getNom());
        verify(prefSportiveRepository).findById(1L);
    }

    @Test
    void trouverParNomRetourneLaPreferenceSiPresente() {
        PrefSportive prefSportive = PrefSportive.builder().id(1L).nom("Yoga").build();

        when(prefSportiveRepository.findByNom("Yoga")).thenReturn(Optional.of(prefSportive));

        Optional<PrefSportive> resultat = prefSportiveService.trouverParNom("Yoga");

        assertTrue(resultat.isPresent());
        assertEquals("Yoga", resultat.get().getNom());
        verify(prefSportiveRepository).findByNom("Yoga");
    }

    @Test
    void creerPrefSportiveSauvegardeLaPreference() {
        PrefSportive prefSportive = PrefSportive.builder().nom("Yoga").build();

        when(prefSportiveRepository.save(any(PrefSportive.class))).thenAnswer(invocation -> {
            PrefSportive saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        PrefSportive resultat = prefSportiveService.creerPrefSportive(prefSportive);

        assertNotNull(resultat.getId());
        assertEquals("Yoga", resultat.getNom());
        verify(prefSportiveRepository).save(any(PrefSportive.class));
    }

    @Test
    void modifierPrefSportiveMetAJourLaPreference() {
        PrefSportive existante = PrefSportive.builder().id(1L).nom("Yoga").build();
        PrefSportive updates = PrefSportive.builder().nom("Course").build();

        when(prefSportiveRepository.findById(1L)).thenReturn(Optional.of(existante));
        when(prefSportiveRepository.save(any(PrefSportive.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PrefSportive resultat = prefSportiveService.modifierPrefSportive(1L, updates);

        assertEquals("Course", resultat.getNom());
        verify(prefSportiveRepository).findById(1L);
        verify(prefSportiveRepository).save(existante);
    }

    @Test
    void modifierPrefSportiveRefuseIdInexistant() {
        PrefSportive updates = PrefSportive.builder().nom("Course").build();

        when(prefSportiveRepository.findById(1L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> prefSportiveService.modifierPrefSportive(1L, updates));

        assertEquals("PrefSportive introuvable : 1", exception.getMessage());
        verify(prefSportiveRepository).findById(1L);
    }

    @Test
    void supprimerPrefSportiveSupprimeSiExiste() {
        when(prefSportiveRepository.existsById(1L)).thenReturn(true);

        prefSportiveService.supprimerPrefSportive(1L);

        verify(prefSportiveRepository).existsById(1L);
        verify(prefSportiveRepository).deleteById(1L);
    }

    @Test
    void supprimerPrefSportiveRefuseIdInexistant() {
        when(prefSportiveRepository.existsById(1L)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> prefSportiveService.supprimerPrefSportive(1L));

        assertEquals("PrefSportive introuvable : 1", exception.getMessage());
        verify(prefSportiveRepository).existsById(1L);
    }
}
