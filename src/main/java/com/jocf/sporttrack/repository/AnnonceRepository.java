package com.jocf.sporttrack.repository;

import com.jocf.sporttrack.model.Annonce;
import com.jocf.sporttrack.model.Evenement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnnonceRepository extends JpaRepository<Annonce, Long> {

    // Toutes les annonces d'un événement, triées par date décroissante
    List<Annonce> findByEvenementOrderByDateDesc(Evenement evenement);

    // Dernière annonce d'un événement
    Optional<Annonce> findTopByEvenementOrderByDateDesc(Evenement evenement);

    List<Annonce> findByEvenement(Evenement evenement);
}
