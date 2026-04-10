package com.jocf.sporttrack.repository;

import com.jocf.sporttrack.model.Annonce;
import com.jocf.sporttrack.model.Evenement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnnonceRepository extends JpaRepository<Annonce, Long> {

    // Toutes les annonces d'un événement, triées par date décroissante

    // Dernière annonce d'un événement

    List<Annonce> findByEvenement(Evenement evenement);

    List<Annonce> findByEvenementOrderByDateDesc(Evenement evenement);

    Optional<Annonce> findTopByEvenementOrderByDateDesc(Evenement evenement);

    @Query("""
            SELECT DISTINCT a FROM Annonce a
            JOIN FETCH a.evenement e
            JOIN e.participants p
            WHERE p.id = :utilisateurId
            ORDER BY a.date DESC
            """)
    List<Annonce> findAnnoncesPourEvenementsOuUtilisateurParticipe(@Param("utilisateurId") Long utilisateurId);
}
