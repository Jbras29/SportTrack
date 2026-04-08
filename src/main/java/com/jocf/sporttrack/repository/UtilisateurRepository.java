package com.jocf.sporttrack.repository;

import com.jocf.sporttrack.model.Utilisateur;
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

    boolean existsByEmail(String email);
}
