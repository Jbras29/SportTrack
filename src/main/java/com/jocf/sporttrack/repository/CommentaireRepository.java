package com.jocf.sporttrack.repository;

import com.jocf.sporttrack.model.Commentaire;
import com.jocf.sporttrack.enumeration.TypeCommentaire;
import com.jocf.sporttrack.model.Activite;
import com.jocf.sporttrack.model.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommentaireRepository extends JpaRepository<Commentaire, Long> {

    List<Commentaire> findByActivite(Activite activite);

    List<Commentaire> findByAuteur(Utilisateur auteur);

    List<Commentaire> findByActiviteOrderByDateCreationDesc(Activite activite);

    Optional<Commentaire> findByActiviteAndAuteurAndTypeAndMessage(
            Activite activite, Utilisateur auteur, TypeCommentaire type, String message);

    /**
     * Réactions ou commentaires sur les activités du propriétaire, hors actions du propriétaire lui-même.
     */
    @Query("""
            SELECT DISTINCT c FROM Commentaire c
            JOIN FETCH c.auteur aut
            JOIN FETCH c.activite act
            JOIN FETCH act.utilisateur prop
            WHERE prop.id = :proprietaireActiviteId
              AND aut.id <> :proprietaireActiviteId
              AND c.type = :type
            ORDER BY c.dateCreation DESC
            """)
    List<Commentaire> findPourProprietaireActiviteEtType(
            @Param("proprietaireActiviteId") Long proprietaireActiviteId,
            @Param("type") TypeCommentaire type);
}