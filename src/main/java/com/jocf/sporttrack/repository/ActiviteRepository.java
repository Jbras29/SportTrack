package com.jocf.sporttrack.repository;

import com.jocf.sporttrack.model.Activite;
import com.jocf.sporttrack.model.TypeSport;
import com.jocf.sporttrack.model.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
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
            WHERE u.id IN :utilisateurIds
            ORDER BY a.date DESC
            """)
    List<Activite> findByUtilisateurIdsWithUtilisateurOrderByDateDesc(
            @Param("utilisateurIds") Collection<Long> utilisateurIds);

    // Toutes les activités d'un utilisateur
    List<Activite> findByUtilisateur(Utilisateur utilisateur);

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

    List<Activite> findTop5ByUtilisateurOrderByDateDesc(Utilisateur utilisateur);

    // Filtrage avancé multi-critères via JPQL
    @Query("""
            SELECT a FROM Activite a
            WHERE (:utilisateur IS NULL OR a.utilisateur = :utilisateur)
              AND (:typeSport   IS NULL OR a.typeSport = :typeSport)
              AND (:distanceMin IS NULL OR a.distance >= :distanceMin)
              AND (:distanceMax IS NULL OR a.distance <= :distanceMax)
              AND (:tempsMin    IS NULL OR a.temps >= :tempsMin)
              AND (:tempsMax    IS NULL OR a.temps <= :tempsMax)
              AND (:dateDebut   IS NULL OR a.date >= :dateDebut)
              AND (:dateFin     IS NULL OR a.date <= :dateFin)
              AND (:location    IS NULL OR LOWER(a.location) LIKE LOWER(CONCAT('%', :location, '%')))
            ORDER BY a.date DESC
            """)
    List<Activite> filtrer(
            @Param("utilisateur") Utilisateur utilisateur,
            @Param("typeSport")   TypeSport typeSport,
            @Param("distanceMin") Double distanceMin,
            @Param("distanceMax") Double distanceMax,
            @Param("tempsMin")    Integer tempsMin,
            @Param("tempsMax")    Integer tempsMax,
            @Param("dateDebut")   LocalDate dateDebut,
            @Param("dateFin")     LocalDate dateFin,
            @Param("location")    String location
    );
}
