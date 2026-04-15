package com.jocf.sporttrack.controller;

import com.jocf.sporttrack.dto.CommentaireFeedbackResponse;
import com.jocf.sporttrack.dto.CreerCommentaireTexteRequest;
import com.jocf.sporttrack.dto.CreerReactionRequest;
import com.jocf.sporttrack.enumeration.TypeCommentaire;
import com.jocf.sporttrack.model.Commentaire;
import com.jocf.sporttrack.service.CommentaireService;
import com.jocf.sporttrack.service.NonAutoriseException;
import com.jocf.sporttrack.service.ReactionAjoutResultat;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentaireControllerTest {

    @Mock
    private CommentaireService commentaireService;

    @InjectMocks
    private CommentaireController controller;

    private Commentaire commentaire;

    @BeforeEach
    void setUp() {
        commentaire = Commentaire.builder()
                .id(5L)
                .type(TypeCommentaire.MESSAGE)
                .message("Salut")
                .build();
    }

    @Test
    void getAllCommentaires_retourneListe() {
        when(commentaireService.recupererTousLesCommentaires()).thenReturn(List.of(commentaire));

        var response = controller.getAllCommentaires();

        assertThat(response.getBody()).containsExactly(commentaire);
    }

    @Test
    void getCommentaireById_retourneLeCommentaireQuandIlExiste() {
        when(commentaireService.trouverParId(5L)).thenReturn(Optional.of(commentaire));

        var response = controller.getCommentaireById(5L);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(commentaire);
    }

    @Test
    void getCommentaireById_retourne404SiAbsent() {
        when(commentaireService.trouverParId(5L)).thenReturn(Optional.empty());

        var response = controller.getCommentaireById(5L);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void getCommentairesByActivite_retourneListeQuandDisponible() {
        when(commentaireService.recupererCommentairesParActivite(3L)).thenReturn(List.of(commentaire));

        var response = controller.getCommentairesByActivite(3L);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsExactly(commentaire);
    }

    @Test
    void getCommentairesByActivite_retourne404QuandActiviteIntrouvable() {
        when(commentaireService.recupererCommentairesParActivite(3L))
                .thenThrow(new IllegalArgumentException("introuvable"));

        var response = controller.getCommentairesByActivite(3L);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void createCommentaire_retourne400QuandErreurValidation() {
        when(commentaireService.creerCommentaire(1L, 2L, TypeCommentaire.MESSAGE, "x"))
                .thenThrow(new IllegalArgumentException("bad"));

        var response = controller.createCommentaire(1L, 2L, TypeCommentaire.MESSAGE, "x");

        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void createCommentaire_retourneLeCommentaireCree() {
        when(commentaireService.creerCommentaire(1L, 2L, TypeCommentaire.MESSAGE, "x"))
                .thenReturn(commentaire);

        var response = controller.createCommentaire(1L, 2L, TypeCommentaire.MESSAGE, "x");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(commentaire);
    }

    @Test
    void posterCommentaireTexte_retourne400SiBodySansAuteur() {
        var response = controller.posterCommentaireTexte(2L, new CreerCommentaireTexteRequest(null, "x"));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isEqualTo(CommentaireFeedbackResponse.erreur("Le corps doit contenir auteurId."));
    }

    @Test
    void posterCommentaireTexte_retourne404QuandActiviteIntrouvable() {
        when(commentaireService.ajouterCommentaireTexte(1L, 2L, "hello"))
                .thenThrow(new IllegalArgumentException("Activite introuvable : 2"));

        var response = controller.posterCommentaireTexte(2L, new CreerCommentaireTexteRequest(1L, "hello"));

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody().message()).isEqualTo("Activite introuvable : 2");
    }

    @Test
    void posterCommentaireTexte_retourne201QuandSucces() {
        when(commentaireService.ajouterCommentaireTexte(1L, 2L, "hello")).thenReturn(commentaire);

        var response = controller.posterCommentaireTexte(2L, new CreerCommentaireTexteRequest(1L, "hello"));

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().commentaireId()).isEqualTo(5L);
    }

    @Test
    void posterReaction_retourne201QuandNouvelleReaction() {
        when(commentaireService.ajouterReactionEmoji(1L, 2L, "👍"))
                .thenReturn(new ReactionAjoutResultat(commentaire, true));

        var response = controller.posterReaction(2L, new CreerReactionRequest(1L, "👍"));

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isEqualTo(CommentaireFeedbackResponse.ok("Réaction enregistrée.", commentaire));
    }

    @Test
    void posterReaction_retourne200QuandDejaPresente() {
        when(commentaireService.ajouterReactionEmoji(1L, 2L, "👍"))
                .thenReturn(new ReactionAjoutResultat(commentaire, false));

        var response = controller.posterReaction(2L, new CreerReactionRequest(1L, "👍"));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().message()).isEqualTo("Réaction déjà présente.");
    }

    @Test
    void posterReaction_retourne400QuandLaReactionEstInvalide() {
        when(commentaireService.ajouterReactionEmoji(1L, 2L, "👍"))
                .thenThrow(new IllegalArgumentException("emoji invalide"));

        var response = controller.posterReaction(2L, new CreerReactionRequest(1L, "👍"));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().message()).isEqualTo("emoji invalide");
    }

    @Test
    void updateCommentaire_retourne404QuandErreur() {
        when(commentaireService.modifierCommentaire(3L, "nouveau"))
                .thenThrow(new IllegalArgumentException("absent"));

        var response = controller.updateCommentaire(3L, "nouveau");

        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void deleteCommentaire_retourne204QuandSucces() {
        var response = controller.deleteCommentaire(3L);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
    }

    @Test
    void supprimerCommentaireTexte_retourne200QuandSucces() {
        var response = controller.supprimerCommentaireTexte(1L, 2L, 3L);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(CommentaireFeedbackResponse.okSansCommentaire("Commentaire supprimé.", 2L));
    }

    @Test
    void supprimerCommentaireTexte_retourne404QuandLeCommentaireNappartientPasALActivite() {
        org.mockito.Mockito.doThrow(new IllegalArgumentException("Commentaire 2 n'appartient pas à cette activité"))
                .when(commentaireService).supprimerCommentaireTexte(1L, 2L, 3L);

        var response = controller.supprimerCommentaireTexte(1L, 2L, 3L);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void supprimerCommentaireTexte_retourne403QuandNonAutorise() {
        org.mockito.Mockito.doThrow(new NonAutoriseException("forbidden"))
                .when(commentaireService).supprimerCommentaireTexte(1L, 2L, 3L);

        var response = controller.supprimerCommentaireTexte(1L, 2L, 3L);

        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(response.getBody().message()).isEqualTo("forbidden");
    }

    @Test
    void supprimerReaction_retourne404QuandCommentaireIntrouvable() {
        org.mockito.Mockito.doThrow(new IllegalArgumentException("Commentaire introuvable : 2"))
                .when(commentaireService).supprimerReaction(1L, 2L, 3L);

        var response = controller.supprimerReaction(1L, 2L, 3L);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void supprimerReaction_retourne200QuandSucces() {
        var response = controller.supprimerReaction(1L, 2L, 3L);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(CommentaireFeedbackResponse.okSansCommentaire("Réaction supprimée.", 2L));
    }
}
