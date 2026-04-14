package com.jocf.sporttrack.repository;

import com.jocf.sporttrack.dto.ActiviteFiltre;
import com.jocf.sporttrack.model.Activite;
import com.jocf.sporttrack.model.TypeSport;
import com.jocf.sporttrack.model.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ActiviteRepository extends JpaRepository<Activite, Long> {

    @Query("""
            SELECT DISTINCT a FROM Activite a
            JOIN FETCH a.utilisateur u
            LEFT JOIN FETCH a.invites
            WHERE u.id IN :utilisateurIds
            ORDER BY a.date DESC
            """)
    List<Activite> findByUtilisateurIdsWithUtilisateurOrderByDateDesc(
            @Param("utilisateurIds") Collection<Long> utilisateurIds);

    // Toutes les activités d'un utilisateur
    @EntityGraph(attributePaths = {"utilisateur", "invites"})
    List<Activite> findByUtilisateur(Utilisateur utilisateur);

    @EntityGraph(attributePaths = {"utilisateur", "invites"})
    List<Activite> findByUtilisateurOrderByDateDesc(Utilisateur utilisateur);

    // Une activité par son id (déjà fourni par JpaRepository via findById)
    Optional<Activite> findById(Long id);

    // Dernière activité d'un utilisateur (par date décroissante)
    Optional<Activite> findTopByUtilisateurOrderByDateDesc(Utilisateur utilisateur);

    // Activités par type de sport
    List<Activite> findByTypeSport(TypeSport typeSport);

    // Activités par type de sport pour un utilisateur donné
    List<Activite> findByUtilisateurAndTypeSport(Utilisateur utilisateur, TypeSport typeSport);

    // Filtrage par distance (entre min et max)
    List<Activite> findByDistanceBetween(Double distanceMin, Double distanceMax);

    // Filtrage par distance min
    List<Activite> findByDistanceGreaterThanEqual(Double distanceMin);

    // Filtrage par temps (entre min et max, en minutes)
    List<Activite> findByTempsBetween(Integer tempsMin, Integer tempsMax);

    // Filtrage par plage de dates
    List<Activite> findByDateBetween(LocalDate dateDebut, LocalDate dateFin);

    // Filtrage par location (recherche partielle, insensible à la casse)
    List<Activite> findByLocationContainingIgnoreCase(String location);

    // Filtrage combiné : utilisateur + type de sport + plage de dates
    List<Activite> findByUtilisateurAndTypeSportAndDateBetween(
            Utilisateur utilisateur,
            TypeSport typeSport,
            LocalDate dateDebut,
            LocalDate dateFin
    );

    // Filtrage combiné : utilisateur + plage de dates
    List<Activite> findByUtilisateurAndDateBetween(
            Utilisateur utilisateur,
            LocalDate dateDebut,
            LocalDate dateFin
    );

    long countByUtilisateur(Utilisateur utilisateur);

    @Query("SELECT COALESCE(SUM(a.distance), 0) FROM Activite a WHERE a.utilisateur = :utilisateur")
    Double sumDistanceByUtilisateur(@Param("utilisateur") Utilisateur utilisateur);

    @Query("SELECT COALESCE(SUM(a.temps), 0) FROM Activite a WHERE a.utilisateur = :utilisateur")
    Integer sumTempsMinutesByUtilisateur(@Param("utilisateur") Utilisateur utilisateur);

    @EntityGraph(attributePaths = {"utilisateur", "invites"})
    List<Activite> findTop5ByUtilisateurOrderByDateDesc(Utilisateur utilisateur);

    // Filtrage avancé multi-critères via JPQL (critères groupés dans {@link ActiviteFiltre})
    @Query("""
            SELECT a FROM Activite a
            WHERE (:#{#f.utilisateur} IS NULL OR a.utilisateur = :#{#f.utilisateur})
              AND (:#{#f.typeSport} IS NULL OR a.typeSport = :#{#f.typeSport})
              AND (:#{#f.distanceMin} IS NULL OR a.distance >= :#{#f.distanceMin})
              AND (:#{#f.distanceMax} IS NULL OR a.distance <= :#{#f.distanceMax})
              AND (:#{#f.tempsMin} IS NULL OR a.temps >= :#{#f.tempsMin})
              AND (:#{#f.tempsMax} IS NULL OR a.temps <= :#{#f.tempsMax})
              AND (:#{#f.dateDebut} IS NULL OR a.date >= :#{#f.dateDebut})
              AND (:#{#f.dateFin} IS NULL OR a.date <= :#{#f.dateFin})
              AND (:#{#f.location} IS NULL OR LOWER(a.location) LIKE LOWER(CONCAT('%', :#{#f.location}, '%')))
            ORDER BY a.date DESC
            """)
    List<Activite> filtrer(@Param("f") ActiviteFiltre f);
}
