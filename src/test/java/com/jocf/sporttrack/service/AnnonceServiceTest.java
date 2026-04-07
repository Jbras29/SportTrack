package com.jocf.sporttrack.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jocf.sporttrack.model.Annonce;
import com.jocf.sporttrack.model.Evenement;
import com.jocf.sporttrack.repository.AnnonceRepository;
import com.jocf.sporttrack.repository.EvenementRepository;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnnonceServiceTest {

    @Mock
    private AnnonceRepository annonceRepository;

    @Mock
    private EvenementRepository evenementRepository;

    private AnnonceService annonceService;

    @BeforeEach
    void setUp() {
        annonceService = new AnnonceService(annonceRepository, evenementRepository);
    }

    @Test
    void recupererToutesLesAnnoncesRetourneLaListe() {
        Annonce annonce1 = Annonce.builder().id(1L).message("Annonce 1").build();
        Annonce annonce2 = Annonce.builder().id(2L).message("Annonce 2").build();
        List<Annonce> annonces = Arrays.asList(annonce1, annonce2);

        when(annonceRepository.findAll()).thenReturn(annonces);

        List<Annonce> resultat = annonceService.recupererToutesLesAnnonces();

        assertEquals(2, resultat.size());
        assertEquals("Annonce 1", resultat.get(0).getMessage());
        assertEquals("Annonce 2", resultat.get(1).getMessage());
        verify(annonceRepository).findAll();
    }

    @Test
    void trouverParIdRetourneLAnnonceSiPresente() {
        Annonce annonce = Annonce.builder().id(1L).message("Test Annonce").build();

        when(annonceRepository.findById(1L)).thenReturn(Optional.of(annonce));

        Optional<Annonce> resultat = annonceService.trouverParId(1L);

        assertTrue(resultat.isPresent());
        assertEquals("Test Annonce", resultat.get().getMessage());
        verify(annonceRepository).findById(1L);
    }

    @Test
    void recupererAnnoncesParEvenementRetourneLaListe() {
        Evenement evenement = Evenement.builder().id(1L).build();
        Annonce annonce1 = Annonce.builder().id(1L).message("Annonce 1").build();
        Annonce annonce2 = Annonce.builder().id(2L).message("Annonce 2").build();
        List<Annonce> annonces = Arrays.asList(annonce1, annonce2);

        when(evenementRepository.findById(1L)).thenReturn(Optional.of(evenement));
        when(annonceRepository.findByEvenement(evenement)).thenReturn(annonces);

        List<Annonce> resultat = annonceService.recupererAnnoncesParEvenement(1L);

        assertEquals(2, resultat.size());
        verify(evenementRepository).findById(1L);
        verify(annonceRepository).findByEvenement(evenement);
    }

    @Test
    void recupererAnnoncesParEvenementRefuseEvenementInexistant() {
        when(evenementRepository.findById(1L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> annonceService.recupererAnnoncesParEvenement(1L));

        assertEquals("Evenement introuvable : 1", exception.getMessage());
        verify(evenementRepository).findById(1L);
    }

    @Test
    void creerAnnonceAvecEvenementValide() {
        Evenement evenement = Evenement.builder().id(1L).build();

        when(evenementRepository.findById(1L)).thenReturn(Optional.of(evenement));
        when(annonceRepository.save(any(Annonce.class))).thenAnswer(invocation -> {
            Annonce saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        Annonce resultat = annonceService.creerAnnonce(1L, "Nouvelle annonce");

        assertNotNull(resultat.getId());
        assertEquals("Nouvelle annonce", resultat.getMessage());
        assertEquals(evenement, resultat.getEvenement());
        assertNotNull(resultat.getDate());
        verify(evenementRepository).findById(1L);
        verify(annonceRepository).save(any(Annonce.class));
    }

    @Test
    void creerAnnonceRefuseEvenementInexistant() {
        when(evenementRepository.findById(1L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> annonceService.creerAnnonce(1L, "Message"));

        assertEquals("Evenement introuvable : 1", exception.getMessage());
        verify(evenementRepository).findById(1L);
    }

    @Test
    void modifierAnnonceMetAJourLeMessage() {
        Annonce annonce = Annonce.builder()
                .id(1L)
                .message("Ancien message")
                .build();

        when(annonceRepository.findById(1L)).thenReturn(Optional.of(annonce));
        when(annonceRepository.save(any(Annonce.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Annonce resultat = annonceService.modifierAnnonce(1L, "Nouveau message");

        assertEquals("Nouveau message", resultat.getMessage());
        verify(annonceRepository).findById(1L);
        verify(annonceRepository).save(annonce);
    }

    @Test
    void modifierAnnonceRefuseIdInexistant() {
        when(annonceRepository.findById(1L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> annonceService.modifierAnnonce(1L, "Message"));

        assertEquals("Annonce introuvable : 1", exception.getMessage());
        verify(annonceRepository).findById(1L);
    }

    @Test
    void supprimerAnnonceSupprimeSiExiste() {
        when(annonceRepository.existsById(1L)).thenReturn(true);

        annonceService.supprimerAnnonce(1L);

        verify(annonceRepository).existsById(1L);
        verify(annonceRepository).deleteById(1L);
    }

    @Test
    void supprimerAnnonceRefuseIdInexistant() {
        when(annonceRepository.existsById(1L)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> annonceService.supprimerAnnonce(1L));

        assertEquals("Annonce introuvable : 1", exception.getMessage());
        verify(annonceRepository).existsById(1L);
    }
}