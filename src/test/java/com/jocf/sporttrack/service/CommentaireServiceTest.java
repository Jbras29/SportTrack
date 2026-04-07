package com.jocf.sporttrack.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jocf.sporttrack.model.Activite;
import com.jocf.sporttrack.model.Commentaire;
import com.jocf.sporttrack.model.TypeCommentaire;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.repository.ActiviteRepository;
import com.jocf.sporttrack.repository.CommentaireRepository;
import com.jocf.sporttrack.repository.UtilisateurRepository;
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
class CommentaireServiceTest {

    @Mock
    private CommentaireRepository commentaireRepository;

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private ActiviteRepository activiteRepository;

    private CommentaireService commentaireService;

    @BeforeEach
    void setUp() {
        commentaireService = new CommentaireService(commentaireRepository, utilisateurRepository, activiteRepository);
    }

    @Test
    void recupererTousLesCommentairesRetourneLaListe() {
        Commentaire commentaire1 = Commentaire.builder().id(1L).message("Comment 1").build();
        Commentaire commentaire2 = Commentaire.builder().id(2L).message("Comment 2").build();
        List<Commentaire> commentaires = Arrays.asList(commentaire1, commentaire2);

        when(commentaireRepository.findAll()).thenReturn(commentaires);

        List<Commentaire> resultat = commentaireService.recupererTousLesCommentaires();

        assertEquals(2, resultat.size());
        assertEquals("Comment 1", resultat.get(0).getMessage());
        assertEquals("Comment 2", resultat.get(1).getMessage());
        verify(commentaireRepository).findAll();
    }

    @Test
    void trouverParIdRetourneLeCommentaireSiPresent() {
        Commentaire commentaire = Commentaire.builder().id(1L).message("Test Comment").build();

        when(commentaireRepository.findById(1L)).thenReturn(Optional.of(commentaire));

        Optional<Commentaire> resultat = commentaireService.trouverParId(1L);

        assertTrue(resultat.isPresent());
        assertEquals("Test Comment", resultat.get().getMessage());
        verify(commentaireRepository).findById(1L);
    }

    @Test
    void trouverParIdRetourneVideSiAbsent() {
        when(commentaireRepository.findById(1L)).thenReturn(Optional.empty());

        Optional<Commentaire> resultat = commentaireService.trouverParId(1L);

        assertTrue(resultat.isEmpty());
        verify(commentaireRepository).findById(1L);
    }

    @Test
    void recupererCommentairesParActiviteRetourneLaListeTriee() {
        Utilisateur auteur = Utilisateur.builder().id(1L).build();
        Activite activite = Activite.builder().id(1L).build();
        Commentaire commentaire1 = Commentaire.builder().id(1L).message("Comment 1").dateCreation(LocalDateTime.now().minusHours(1)).build();
        Commentaire commentaire2 = Commentaire.builder().id(2L).message("Comment 2").dateCreation(LocalDateTime.now()).build();
        List<Commentaire> commentaires = Arrays.asList(commentaire1, commentaire2);

        when(activiteRepository.findById(1L)).thenReturn(Optional.of(activite));
        when(commentaireRepository.findByActiviteOrderByDateCreationDesc(activite)).thenReturn(commentaires);

        List<Commentaire> resultat = commentaireService.recupererCommentairesParActivite(1L);

        assertEquals(2, resultat.size());
        verify(activiteRepository).findById(1L);
        verify(commentaireRepository).findByActiviteOrderByDateCreationDesc(activite);
    }

    @Test
    void recupererCommentairesParActiviteRefuseActiviteInexistante() {
        when(activiteRepository.findById(1L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> commentaireService.recupererCommentairesParActivite(1L));

        assertEquals("Activite introuvable : 1", exception.getMessage());
        verify(activiteRepository).findById(1L);
    }

    @Test
    void creerCommentaireAvecAuteurEtActiviteValides() {
        Utilisateur auteur = Utilisateur.builder().id(1L).username("auteur").build();
        Activite activite = Activite.builder().id(1L).build();

        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(auteur));
        when(activiteRepository.findById(1L)).thenReturn(Optional.of(activite));
        when(commentaireRepository.save(any(Commentaire.class))).thenAnswer(invocation -> {
            Commentaire saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        Commentaire resultat = commentaireService.creerCommentaire(1L, 1L, TypeCommentaire.MESSAGE, "Super activité !");

        assertNotNull(resultat.getId());
        assertEquals(TypeCommentaire.MESSAGE, resultat.getType());
        assertEquals("Super activité !", resultat.getMessage());
        assertEquals(auteur, resultat.getAuteur());
        assertEquals(activite, resultat.getActivite());
        assertNotNull(resultat.getDateCreation());
        verify(utilisateurRepository).findById(1L);
        verify(activiteRepository).findById(1L);
        verify(commentaireRepository).save(any(Commentaire.class));
    }

    @Test
    void creerCommentaireRefuseAuteurInexistant() {
        when(utilisateurRepository.findById(1L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> commentaireService.creerCommentaire(1L, 1L, TypeCommentaire.MESSAGE, "Message"));

        assertEquals("Auteur introuvable : 1", exception.getMessage());
        verify(utilisateurRepository).findById(1L);
    }

    @Test
    void creerCommentaireRefuseActiviteInexistante() {
        Utilisateur auteur = Utilisateur.builder().id(1L).build();

        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(auteur));
        when(activiteRepository.findById(1L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> commentaireService.creerCommentaire(1L, 1L, TypeCommentaire.MESSAGE, "Message"));

        assertEquals("Activite introuvable : 1", exception.getMessage());
        verify(utilisateurRepository).findById(1L);
        verify(activiteRepository).findById(1L);
    }

    @Test
    void modifierCommentaireMetAJourLeMessage() {
        Commentaire commentaire = Commentaire.builder()
                .id(1L)
                .message("Ancien message")
                .build();

        when(commentaireRepository.findById(1L)).thenReturn(Optional.of(commentaire));
        when(commentaireRepository.save(any(Commentaire.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Commentaire resultat = commentaireService.modifierCommentaire(1L, "Nouveau message");

        assertEquals("Nouveau message", resultat.getMessage());
        verify(commentaireRepository).findById(1L);
        verify(commentaireRepository).save(commentaire);
    }

    @Test
    void modifierCommentaireRefuseIdInexistant() {
        when(commentaireRepository.findById(1L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> commentaireService.modifierCommentaire(1L, "Message"));

        assertEquals("Commentaire introuvable : 1", exception.getMessage());
        verify(commentaireRepository).findById(1L);
    }

    @Test
    void supprimerCommentaireSupprimeSiExiste() {
        when(commentaireRepository.existsById(1L)).thenReturn(true);

        commentaireService.supprimerCommentaire(1L);

        verify(commentaireRepository).existsById(1L);
        verify(commentaireRepository).deleteById(1L);
    }

    @Test
    void supprimerCommentaireRefuseIdInexistant() {
        when(commentaireRepository.existsById(1L)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> commentaireService.supprimerCommentaire(1L));

        assertEquals("Commentaire introuvable : 1", exception.getMessage());
        verify(commentaireRepository).existsById(1L);
    }
}