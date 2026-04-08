package com.jocf.sporttrack.repository;

import com.jocf.sporttrack.model.Utilisateur;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    Optional<Utilisateur> findByEmail(String email);

    boolean existsByEmail(String email);
    List<Utilisateur> findByPrenomContainingIgnoreCase(String prenom);
}
