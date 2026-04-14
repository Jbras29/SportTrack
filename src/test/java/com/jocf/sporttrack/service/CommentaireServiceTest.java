package com.jocf.sporttrack.service;

import com.jocf.sporttrack.model.Activite;
import com.jocf.sporttrack.model.Commentaire;
import com.jocf.sporttrack.model.TypeCommentaire;
import com.jocf.sporttrack.model.TypeSport;
import com.jocf.sporttrack.model.TypeUtilisateur;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.repository.ActiviteRepository;
import com.jocf.sporttrack.repository.CommentaireRepository;
import com.jocf.sporttrack.repository.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentaireServiceTest {

    private static final Long AUTEUR_ID = 1L;
    private static final Long ACTIVITE_ID = 10L;
    private static final Long COMMENTAIRE_ID = 100L;

    @Mock
    private CommentaireRepository commentaireRepository;
    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private ActiviteRepository activiteRepository;

    @InjectMocks
    private CommentaireService commentaireService;

    @BeforeEach
    void lenientSave() {
        lenient().when(commentaireRepository.save(any(Commentaire.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Utilisateur auteur() {
        return Utilisateur.builder()
                .id(AUTEUR_ID)
                .nom("D")
                .prenom("J")
                .email("j@test.com")
                .motdepasse("x")
                .typeUtilisateur(TypeUtilisateur.UTILISATEUR)
                .build();
    }

    private Activite activite() {
        return Activite.builder()
                .id(ACTIVITE_ID)
                .nom("Course")
                .typeSport(TypeSport.COURSE_A_PIED)
                .date(LocalDate.now())
                .utilisateur(auteur())
                .build();
    }

    private Commentaire commentairePersiste(TypeCommentaire type, String msg) {
        return Commentaire.builder()
                .id(COMMENTAIRE_ID)
                .type(type)
                .message(msg)
                .dateCreation(LocalDateTime.now())
                .auteur(auteur())
                .activite(activite())
                .build();
    }

    @Test
    void recupererTousLesCommentaires_delegueAuRepository() {
        Commentaire c = commentairePersiste(TypeCommentaire.MESSAGE, "hi");
        when(commentaireRepository.findAll()).thenReturn(List.of(c));

        assertThat(commentaireService.recupererTousLesCommentaires()).containsExactly(c);
    }

    @Test
    void trouverParId_delegueAuRepository() {
        Commentaire c = commentairePersiste(TypeCommentaire.MESSAGE, "x");
        when(commentaireRepository.findById(COMMENTAIRE_ID)).thenReturn(Optional.of(c));

        assertThat(commentaireService.trouverParId(COMMENTAIRE_ID)).contains(c);
    }

    @Nested
    class RecupererCommentairesParActivite {

        @Test
        void activiteIntrouvable() {
            when(activiteRepository.findById(ACTIVITE_ID)).thenReturn(Optional.empty());
            Executable appel = () -> commentaireService.recupererCommentairesParActivite(ACTIVITE_ID);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, appel);
            assertThat(ex.getMessage()).contains("Activite introuvable");
        }

        @Test
        void ok() {
            Activite a = activite();
            when(activiteRepository.findById(ACTIVITE_ID)).thenReturn(Optional.of(a));
            Commentaire c = commentairePersiste(TypeCommentaire.MESSAGE, "m");
            when(commentaireRepository.findByActiviteOrderByDateCreationDesc(a)).thenReturn(List.of(c));

            assertThat(commentaireService.recupererCommentairesParActivite(ACTIVITE_ID)).containsExactly(c);
        }
    }

    @Nested
    class RecupererCommentairesParAuteur {

        @Test
        void auteurIntrouvable() {
            when(utilisateurRepository.findById(AUTEUR_ID)).thenReturn(Optional.empty());
            Executable appel = () -> commentaireService.recupererCommentairesParAuteur(AUTEUR_ID);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, appel);
            assertThat(ex.getMessage()).contains("Auteur introuvable");
        }

        @Test
        void ok() {
            Utilisateur u = auteur();
            when(utilisateurRepository.findById(AUTEUR_ID)).thenReturn(Optional.of(u));
            Commentaire c = commentairePersiste(TypeCommentaire.MESSAGE, "m");
            when(commentaireRepository.findByAuteur(u)).thenReturn(List.of(c));

            assertThat(commentaireService.recupererCommentairesParAuteur(AUTEUR_ID)).containsExactly(c);
        }
    }

    @Nested
    class AjouterCommentaireTexte {

        @Test
        void messageVide() {
            Executable appel = () -> commentaireService.ajouterCommentaireTexte(AUTEUR_ID, ACTIVITE_ID, "   ");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, appel);
            assertThat(ex.getMessage()).contains("vide");
            verify(utilisateurRepository, never()).findById(any());
            verify(commentaireRepository, never()).save(any());
        }

        @Test
        void messageTropLong() {
            String tropLong = "x".repeat(1001);
            Executable appel = () -> commentaireService.ajouterCommentaireTexte(AUTEUR_ID, ACTIVITE_ID, tropLong);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, appel);
            assertThat(ex.getMessage()).contains("trop long");
            verify(utilisateurRepository, never()).findById(any());
        }

        @Test
        void ok_trimEtPersiste() {
            when(utilisateurRepository.findById(AUTEUR_ID)).thenReturn(Optional.of(auteur()));
            when(activiteRepository.findById(ACTIVITE_ID)).thenReturn(Optional.of(activite()));

            commentaireService.ajouterCommentaireTexte(AUTEUR_ID, ACTIVITE_ID, "  hello  ");

            verify(commentaireRepository).save(any(Commentaire.class));
        }
    }

    @Nested
    class AjouterReactionEmoji {

        @Test
        void emojiVide() {
            when(utilisateurRepository.findById(AUTEUR_ID)).thenReturn(Optional.of(auteur()));
            when(activiteRepository.findById(ACTIVITE_ID)).thenReturn(Optional.of(activite()));
            Executable appel = () -> commentaireService.ajouterReactionEmoji(AUTEUR_ID, ACTIVITE_ID, " ");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, appel);
            assertThat(ex.getMessage()).contains("emoji");
        }

        @Test
        void emojiTropLong() {
            when(utilisateurRepository.findById(AUTEUR_ID)).thenReturn(Optional.of(auteur()));
            when(activiteRepository.findById(ACTIVITE_ID)).thenReturn(Optional.of(activite()));
            String tropLong = "x".repeat(33);
            Executable appel = () -> commentaireService.ajouterReactionEmoji(AUTEUR_ID, ACTIVITE_ID, tropLong);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, appel);
            assertThat(ex.getMessage()).contains("trop long");
        }

        @Test
        void reactionExistante_renvoiExistant_sansNouveauSave() {
            Utilisateur u = auteur();
            Activite a = activite();
            when(utilisateurRepository.findById(AUTEUR_ID)).thenReturn(Optional.of(u));
            when(activiteRepository.findById(ACTIVITE_ID)).thenReturn(Optional.of(a));
            Commentaire existant = commentairePersiste(TypeCommentaire.REACTION, "👍");
            when(commentaireRepository.findByActiviteAndAuteurAndTypeAndMessage(
                    a, u, TypeCommentaire.REACTION, "👍")).thenReturn(Optional.of(existant));

            ReactionAjoutResultat r = commentaireService.ajouterReactionEmoji(AUTEUR_ID, ACTIVITE_ID, "👍");

            assertThat(r.commentaire()).isSameAs(existant);
            assertThat(r.nouvellementCree()).isFalse();
            verify(commentaireRepository, never()).save(any());
        }

        @Test
        void nouvelleReaction_nouvellementCreeTrue() {
            Utilisateur u = auteur();
            Activite a = activite();
            when(utilisateurRepository.findById(AUTEUR_ID)).thenReturn(Optional.of(u));
            when(activiteRepository.findById(ACTIVITE_ID)).thenReturn(Optional.of(a));
            when(commentaireRepository.findByActiviteAndAuteurAndTypeAndMessage(
                    a, u, TypeCommentaire.REACTION, "🔥")).thenReturn(Optional.empty());

            ReactionAjoutResultat r = commentaireService.ajouterReactionEmoji(AUTEUR_ID, ACTIVITE_ID, "🔥");

            assertThat(r.nouvellementCree()).isTrue();
            assertThat(r.commentaire().getMessage()).isEqualTo("🔥");
            assertThat(r.commentaire().getType()).isEqualTo(TypeCommentaire.REACTION);
            verify(commentaireRepository).save(any(Commentaire.class));
        }
    }

    @Nested
    class SupprimerCommentaireTexte {

        @Test
        void mauvaisType() {
            Commentaire c = commentairePersiste(TypeCommentaire.REACTION, "👍");
            when(commentaireRepository.findById(COMMENTAIRE_ID)).thenReturn(Optional.of(c));
            Executable appel = () ->
                    commentaireService.supprimerCommentaireTexte(ACTIVITE_ID, COMMENTAIRE_ID, AUTEUR_ID);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, appel);
            assertThat(ex.getMessage()).contains("textuel");
            verify(commentaireRepository, never()).delete(any());
        }

        @Test
        void mauvaiseActivite() {
            Commentaire c = commentairePersiste(TypeCommentaire.MESSAGE, "hi");
            when(commentaireRepository.findById(COMMENTAIRE_ID)).thenReturn(Optional.of(c));
            Executable appel = () ->
                    commentaireService.supprimerCommentaireTexte(999L, COMMENTAIRE_ID, AUTEUR_ID);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, appel);
            assertThat(ex.getMessage()).contains("activité");
        }

        @Test
        void pasAuteur() {
            Commentaire c = commentairePersiste(TypeCommentaire.MESSAGE, "hi");
            Utilisateur autre = Utilisateur.builder()
                    .id(99L)
                    .nom("X")
                    .prenom("Y")
                    .email("y@test.com")
                    .motdepasse("x")
                    .typeUtilisateur(TypeUtilisateur.UTILISATEUR)
                    .build();
            c.setAuteur(autre);
            when(commentaireRepository.findById(COMMENTAIRE_ID)).thenReturn(Optional.of(c));
            Executable appel = () ->
                    commentaireService.supprimerCommentaireTexte(ACTIVITE_ID, COMMENTAIRE_ID, AUTEUR_ID);

            assertThrows(NonAutoriseException.class, appel);
            verify(commentaireRepository, never()).delete(any());
        }

        @Test
        void ok() {
            Commentaire c = commentairePersiste(TypeCommentaire.MESSAGE, "hi");
            when(commentaireRepository.findById(COMMENTAIRE_ID)).thenReturn(Optional.of(c));

            commentaireService.supprimerCommentaireTexte(ACTIVITE_ID, COMMENTAIRE_ID, AUTEUR_ID);

            verify(commentaireRepository).delete(c);
        }
    }

    @Nested
    class SupprimerReaction {

        @Test
        void mauvaisType() {
            Commentaire c = commentairePersiste(TypeCommentaire.MESSAGE, "texte");
            when(commentaireRepository.findById(COMMENTAIRE_ID)).thenReturn(Optional.of(c));
            Executable appel = () ->
                    commentaireService.supprimerReaction(ACTIVITE_ID, COMMENTAIRE_ID, AUTEUR_ID);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, appel);
            assertThat(ex.getMessage()).contains("réaction");
        }
    }

    @Nested
    class ModifierEtSupprimerCommentaire {

        @Test
        void modifier_introuvable() {
            when(commentaireRepository.findById(COMMENTAIRE_ID)).thenReturn(Optional.empty());
            Executable appel = () -> commentaireService.modifierCommentaire(COMMENTAIRE_ID, "x");

            assertThrows(IllegalArgumentException.class, appel);
        }

        @Test
        void modifier_ok() {
            Commentaire c = commentairePersiste(TypeCommentaire.MESSAGE, "old");
            when(commentaireRepository.findById(COMMENTAIRE_ID)).thenReturn(Optional.of(c));

            Commentaire result = commentaireService.modifierCommentaire(COMMENTAIRE_ID, "new");

            assertThat(result.getMessage()).isEqualTo("new");
            verify(commentaireRepository).save(c);
        }

        @Test
        void supprimerParId_introuvable() {
            when(commentaireRepository.existsById(COMMENTAIRE_ID)).thenReturn(false);
            Executable appel = () -> commentaireService.supprimerCommentaire(COMMENTAIRE_ID);

            assertThrows(IllegalArgumentException.class, appel);
            verify(commentaireRepository, never()).deleteById(any());
        }

        @Test
        void supprimerParId_ok() {
            when(commentaireRepository.existsById(COMMENTAIRE_ID)).thenReturn(true);

            commentaireService.supprimerCommentaire(COMMENTAIRE_ID);

            verify(commentaireRepository).deleteById(COMMENTAIRE_ID);
        }
    }
}
