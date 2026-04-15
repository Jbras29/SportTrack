package com.jocf.sporttrack.service;

import com.jocf.sporttrack.model.Annonce;
import com.jocf.sporttrack.model.Evenement;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.repository.AnnonceRepository;
import com.jocf.sporttrack.repository.EvenementRepository;
import com.jocf.sporttrack.repository.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnnonceServiceTest {

    @Mock
    private AnnonceRepository annonceRepository;

    @Mock
    private EvenementRepository evenementRepository;

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @InjectMocks
    private AnnonceService annonceService;

    private Utilisateur organisateur;
    private Utilisateur participant;
    private Evenement evenement;
    private Annonce annonce;

    @BeforeEach
    void setUp() {
        organisateur = new Utilisateur();
        organisateur.setId(1L);

        participant = new Utilisateur();
        participant.setId(2L);

        evenement = new Evenement();
        evenement.setId(10L);
        evenement.setOrganisateur(organisateur);

        annonce = Annonce.builder()
                .id(100L)
                .message("Annonce test")
                .date(LocalDateTime.now())
                .evenement(evenement)
                .build();
    }

    @Test
    @DisplayName("recupererToutesLesAnnonces retourne toutes les annonces")
    void shouldReturnAllAnnonces() {
        when(annonceRepository.findAll()).thenReturn(List.of(annonce));

        List<Annonce> result = annonceService.recupererToutesLesAnnonces();

        assertEquals(1, result.size());
        assertEquals("Annonce test", result.get(0).getMessage());
        verify(annonceRepository).findAll();
    }

    @Test
    @DisplayName("trouverParId retourne une annonce si elle existe")
    void shouldReturnAnnonceById() {
        when(annonceRepository.findById(100L)).thenReturn(Optional.of(annonce));

        Optional<Annonce> result = annonceService.trouverParId(100L);

        assertTrue(result.isPresent());
        assertEquals(100L, result.get().getId());
        verify(annonceRepository).findById(100L);
    }

    @Test
    @DisplayName("recupererAnnoncesParEvenement retourne les annonces de l'événement")
    void shouldReturnAnnoncesByEvenement() {
        when(evenementRepository.findById(10L)).thenReturn(Optional.of(evenement));
        when(annonceRepository.findByEvenement(evenement)).thenReturn(List.of(annonce));

        List<Annonce> result = annonceService.recupererAnnoncesParEvenement(10L);

        assertEquals(1, result.size());
        assertEquals("Annonce test", result.get(0).getMessage());
        verify(evenementRepository).findById(10L);
        verify(annonceRepository).findByEvenement(evenement);
    }

    @Test
    @DisplayName("recupererAnnoncesParEvenement lance une exception si l'événement n'existe pas")
    void shouldThrowWhenEvenementNotFound() {
        when(evenementRepository.findById(10L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> annonceService.recupererAnnoncesParEvenement(10L)
        );

        assertTrue(exception.getMessage().contains("Evenement introuvable"));
        verify(evenementRepository).findById(10L);
        verify(annonceRepository, never()).findByEvenement(any());
    }

    @Test
    @DisplayName("recupererAnnoncesParParticipant retourne les annonces des événements du participant")
    void shouldReturnAnnoncesByParticipant() {
        Evenement evenement2 = new Evenement();
        evenement2.setId(20L);
        evenement2.setOrganisateur(organisateur);

        Annonce annonce2 = Annonce.builder()
                .id(101L)
                .message("Annonce 2")
                .date(LocalDateTime.now())
                .evenement(evenement2)
                .build();

participant.setEvenementsParticipes(List.of(evenement, evenement2));
        when(utilisateurRepository.findById(2L)).thenReturn(Optional.of(participant));
        when(annonceRepository.findByEvenement(evenement)).thenReturn(List.of(annonce));
        when(annonceRepository.findByEvenement(evenement2)).thenReturn(List.of(annonce2));

        List<Annonce> result = annonceService.recupererAnnoncesParParticipant(2L);

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(a -> a.getMessage().equals("Annonce test")));
        assertTrue(result.stream().anyMatch(a -> a.getMessage().equals("Annonce 2")));
        verify(utilisateurRepository).findById(2L);
        verify(annonceRepository).findByEvenement(evenement);
        verify(annonceRepository).findByEvenement(evenement2);
    }

    @Test
    @DisplayName("recupererAnnoncesParParticipant lance une exception si l'utilisateur n'existe pas")
    void shouldThrowWhenUtilisateurNotFound() {
        when(utilisateurRepository.findById(2L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> annonceService.recupererAnnoncesParParticipant(2L)
        );

        assertTrue(exception.getMessage().contains("Utilisateur introuvable"));
        verify(utilisateurRepository).findById(2L);
        verifyNoInteractions(annonceRepository);
    }

    @Test
    @DisplayName("creerAnnonce crée une annonce si l'utilisateur est l'organisateur")
    void shouldCreateAnnonceWhenUserIsOrganisateur() {
        when(evenementRepository.findById(10L)).thenReturn(Optional.of(evenement));
        when(annonceRepository.save(any(Annonce.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Annonce result = annonceService.creerAnnonce(10L, 1L, "Nouvelle annonce");

        assertNotNull(result);
        assertEquals("Nouvelle annonce", result.getMessage());
        assertEquals(evenement, result.getEvenement());
        assertNotNull(result.getDate());

        verify(evenementRepository).findById(10L);
        verify(annonceRepository).save(any(Annonce.class));
    }

    @Test
    @DisplayName("creerAnnonce lance une exception si l'événement n'existe pas")
    void shouldThrowWhenCreatingAnnonceForUnknownEvenement() {
        when(evenementRepository.findById(10L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> annonceService.creerAnnonce(10L, 1L, "Nouvelle annonce")
        );

        assertTrue(exception.getMessage().contains("Evenement introuvable"));
        verify(evenementRepository).findById(10L);
        verify(annonceRepository, never()).save(any());
    }

    @Test
    @DisplayName("creerAnnonce lance une exception si l'utilisateur n'est pas l'organisateur")
    void shouldThrowWhenUserIsNotOrganisateur() {
        when(evenementRepository.findById(10L)).thenReturn(Optional.of(evenement));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> annonceService.creerAnnonce(10L, 99L, "Nouvelle annonce")
        );

        assertTrue(exception.getMessage().contains("Seul l'organisateur peut publier une annonce"));
        verify(evenementRepository).findById(10L);
        verify(annonceRepository, never()).save(any());
    }

    @Test
    @DisplayName("modifierAnnonce modifie le message et sauvegarde")
    void shouldUpdateAnnonce() {
        when(annonceRepository.findById(100L)).thenReturn(Optional.of(annonce));
        when(annonceRepository.save(any(Annonce.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Annonce result = annonceService.modifierAnnonce(100L, "Message modifié");

        assertEquals("Message modifié", result.getMessage());
        verify(annonceRepository).findById(100L);
        verify(annonceRepository).save(annonce);
    }

    @Test
    @DisplayName("modifierAnnonce lance une exception si l'annonce n'existe pas")
    void shouldThrowWhenUpdatingUnknownAnnonce() {
        when(annonceRepository.findById(100L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> annonceService.modifierAnnonce(100L, "Message modifié")
        );

        assertTrue(exception.getMessage().contains("Annonce introuvable"));
        verify(annonceRepository).findById(100L);
        verify(annonceRepository, never()).save(any());
    }

    @Test
    @DisplayName("supprimerAnnonce supprime l'annonce si elle existe")
    void shouldDeleteAnnonce() {
        when(annonceRepository.existsById(100L)).thenReturn(true);
        doNothing().when(annonceRepository).deleteById(100L);

        annonceService.supprimerAnnonce(100L);

        verify(annonceRepository).existsById(100L);
        verify(annonceRepository).deleteById(100L);
    }

    @Test
    @DisplayName("supprimerAnnonce lance une exception si l'annonce n'existe pas")
    void shouldThrowWhenDeletingUnknownAnnonce() {
        when(annonceRepository.existsById(100L)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> annonceService.supprimerAnnonce(100L)
        );

        assertTrue(exception.getMessage().contains("Annonce introuvable"));
        verify(annonceRepository).existsById(100L);
        verify(annonceRepository, never()).deleteById(anyLong());
    }
}