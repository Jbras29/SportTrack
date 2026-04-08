package com.jocf.sporttrack.repository;

import com.jocf.sporttrack.model.Utilisateur;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    Optional<Utilisateur> findByEmail(String email);

    @Query("SELECT DISTINCT u FROM Utilisateur u LEFT JOIN FETCH u.amis WHERE u.email = :email")
    Optional<Utilisateur> findByEmailWithAmis(@Param("email") String email);

    @Query("""
            SELECT DISTINCT u
            FROM Utilisateur u
            LEFT JOIN FETCH u.prefSportives
            WHERE u.id <> :utilisateurId
              AND (
                    LOWER(u.nom) LIKE LOWER(CONCAT('%', :recherche, '%'))
                 OR LOWER(u.prenom) LIKE LOWER(CONCAT('%', :recherche, '%'))
                 OR LOWER(u.email) LIKE LOWER(CONCAT('%', :recherche, '%'))
              )
            ORDER BY u.prenom, u.nom
            """)
    List<Utilisateur> rechercherPourReseau(@Param("utilisateurId") Long utilisateurId,
            @Param("recherche") String recherche);

    @Query("""
            SELECT DISTINCT u
            FROM Utilisateur u
            LEFT JOIN FETCH u.prefSportives
            """)
    List<Utilisateur> findAllWithPrefSportives();

    @Query("""
            SELECT DISTINCT ami.id
            FROM Utilisateur u
            JOIN u.amis ami
            WHERE u.id = :utilisateurId
            """)
    List<Long> findAmiIdsByUtilisateurId(@Param("utilisateurId") Long utilisateurId);

    @Query("""
            SELECT DISTINCT destinataire.id
            FROM Utilisateur u
            JOIN u.demandesAmisEnvoyees destinataire
            WHERE u.id = :utilisateurId
            """)
    List<Long> findDemandesAmisEnvoyeesIdsByUtilisateurId(@Param("utilisateurId") Long utilisateurId);

    @Query("""
            SELECT DISTINCT expediteur
            FROM Utilisateur expediteur
            JOIN expediteur.demandesAmisEnvoyees destinataire
            WHERE destinataire.id = :utilisateurId
            ORDER BY expediteur.prenom, expediteur.nom
            """)
    List<Utilisateur> findDemandesAmisRecuesByUtilisateurId(@Param("utilisateurId") Long utilisateurId);

    boolean existsByEmail(String email);
    List<Utilisateur> findByPrenomContainingIgnoreCase(String prenom);
}
