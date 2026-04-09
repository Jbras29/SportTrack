package com.jocf.sporttrack.service;

import com.jocf.sporttrack.model.Activite;
import com.jocf.sporttrack.model.Commentaire;
import com.jocf.sporttrack.model.TypeCommentaire;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.repository.ActiviteRepository;
import com.jocf.sporttrack.repository.CommentaireRepository;
import com.jocf.sporttrack.repository.UtilisateurRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CommentaireService {

    private static final int MESSAGE_MAX_LENGTH = 1000;
    private static final int EMOJI_MAX_LENGTH = 32;

    private final CommentaireRepository commentaireRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ActiviteRepository activiteRepository;

    public CommentaireService(CommentaireRepository commentaireRepository,
                              UtilisateurRepository utilisateurRepository,
                              ActiviteRepository activiteRepository) {
        this.commentaireRepository = commentaireRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.activiteRepository = activiteRepository;
    }

    public List<Commentaire> recupererTousLesCommentaires() {
        return commentaireRepository.findAll();
    }

    public Optional<Commentaire> trouverParId(Long id) {
        return commentaireRepository.findById(id);
    }

    public List<Commentaire> recupererCommentairesParActivite(Long activiteId) {
        Activite activite = activiteRepository.findById(activiteId)
                .orElseThrow(() -> new IllegalArgumentException("Activite introuvable : " + activiteId));
        return commentaireRepository.findByActiviteOrderByDateCreationDesc(activite);
    }

    public List<Commentaire> recupererCommentairesParAuteur(Long auteurId) {
        Utilisateur auteur = utilisateurRepository.findById(auteurId)
                .orElseThrow(() -> new IllegalArgumentException("Auteur introuvable : " + auteurId));
        return commentaireRepository.findByAuteur(auteur);
    }

    public Commentaire creerCommentaire(Long auteurId, Long activiteId, TypeCommentaire type, String message) {
        Utilisateur auteur = utilisateurRepository.findById(auteurId)
                .orElseThrow(() -> new IllegalArgumentException("Auteur introuvable : " + auteurId));

        Activite activite = activiteRepository.findById(activiteId)
                .orElseThrow(() -> new IllegalArgumentException("Activite introuvable : " + activiteId));

        Commentaire commentaire = Commentaire.builder()
                .type(type)
                .message(message)
                .dateCreation(LocalDateTime.now())
                .auteur(auteur)
                .activite(activite)
                .build();

        return commentaireRepository.save(commentaire);
    }

    /**
     * Commentaire textuel ({@link TypeCommentaire#MESSAGE}) sur une activité.
     */
    public Commentaire ajouterCommentaireTexte(Long auteurId, Long activiteId, String message) {
        String m = message != null ? message.trim() : "";
        if (m.isEmpty()) {
            throw new IllegalArgumentException("Le message ne peut pas être vide.");
        }
        if (m.length() > MESSAGE_MAX_LENGTH) {
            throw new IllegalArgumentException("Le message est trop long (max " + MESSAGE_MAX_LENGTH + " caractères).");
        }
        return creerCommentaire(auteurId, activiteId, TypeCommentaire.MESSAGE, m);
    }

    /**
     * Réaction emoji ({@link TypeCommentaire#REACTION}) : une entrée par auteur et par emoji sur l'activité.
     * Si la même réaction existe déjà, l'existant est renvoyé avec {@link ReactionAjoutResultat#nouvellementCree} {@code false}.
     */
    public ReactionAjoutResultat ajouterReactionEmoji(Long auteurId, Long activiteId, String emoji) {
        Utilisateur auteur = utilisateurRepository.findById(auteurId)
                .orElseThrow(() -> new IllegalArgumentException("Auteur introuvable : " + auteurId));
        Activite activite = activiteRepository.findById(activiteId)
                .orElseThrow(() -> new IllegalArgumentException("Activite introuvable : " + activiteId));

        String e = emoji != null ? emoji.trim() : "";
        if (e.isEmpty()) {
            throw new IllegalArgumentException("L'emoji ne peut pas être vide.");
        }
        if (e.length() > EMOJI_MAX_LENGTH) {
            throw new IllegalArgumentException("L'emoji est trop long (max " + EMOJI_MAX_LENGTH + " caractères).");
        }

        return commentaireRepository
                .findByActiviteAndAuteurAndTypeAndMessage(activite, auteur, TypeCommentaire.REACTION, e)
                .map(c -> new ReactionAjoutResultat(c, false))
                .orElseGet(() -> new ReactionAjoutResultat(
                        creerCommentaire(auteurId, activiteId, TypeCommentaire.REACTION, e), true));
    }

    public void supprimerCommentaireTexte(Long activiteId, Long commentaireId, Long auteurId) {
        Commentaire c = chargerPourSuppression(activiteId, commentaireId, auteurId);
        if (c.getType() != TypeCommentaire.MESSAGE) {
            throw new IllegalArgumentException("Ce n'est pas un commentaire textuel.");
        }
        commentaireRepository.delete(c);
    }

    public void supprimerReaction(Long activiteId, Long commentaireId, Long auteurId) {
        Commentaire c = chargerPourSuppression(activiteId, commentaireId, auteurId);
        if (c.getType() != TypeCommentaire.REACTION) {
            throw new IllegalArgumentException("Ce n'est pas une réaction.");
        }
        commentaireRepository.delete(c);
    }

    private Commentaire chargerPourSuppression(Long activiteId, Long commentaireId, Long auteurId) {
        Commentaire c = commentaireRepository.findById(commentaireId)
                .orElseThrow(() -> new IllegalArgumentException("Commentaire introuvable : " + commentaireId));
        if (!c.getActivite().getId().equals(activiteId)) {
            throw new IllegalArgumentException("L'entrée n'appartient pas à cette activité.");
        }
        if (!c.getAuteur().getId().equals(auteurId)) {
            throw new NonAutoriseException("Vous n'êtes pas l'auteur de cette entrée.");
        }
        return c;
    }

    public Commentaire modifierCommentaire(Long id, String nouveauMessage) {
        Commentaire commentaire = commentaireRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Commentaire introuvable : " + id));

        commentaire.setMessage(nouveauMessage);
        return commentaireRepository.save(commentaire);
    }

    public void supprimerCommentaire(Long id) {
        if (!commentaireRepository.existsById(id)) {
            throw new IllegalArgumentException("Commentaire introuvable : " + id);
        }
        commentaireRepository.deleteById(id);
    }
}