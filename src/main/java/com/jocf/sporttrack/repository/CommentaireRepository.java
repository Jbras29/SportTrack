package com.jocf.sporttrack.repository;

import com.jocf.sporttrack.model.Commentaire;
import com.jocf.sporttrack.model.Activite;
import com.jocf.sporttrack.model.TypeCommentaire;
import com.jocf.sporttrack.model.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommentaireRepository extends JpaRepository<Commentaire, Long> {

    List<Commentaire> findByActivite(Activite activite);

    List<Commentaire> findByAuteur(Utilisateur auteur);

    List<Commentaire> findByActiviteOrderByDateCreationDesc(Activite activite);

    Optional<Commentaire> findByActiviteAndAuteurAndTypeAndMessage(
            Activite activite, Utilisateur auteur, TypeCommentaire type, String message);
}