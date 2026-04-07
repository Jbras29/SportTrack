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