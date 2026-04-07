package com.jocf.sporttrack.repository;

import com.jocf.sporttrack.model.Evenement;
import com.jocf.sporttrack.model.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EvenementRepository extends JpaRepository<Evenement, Long> {

    List<Evenement> findByOrganisateur(Utilisateur organisateur);

    List<Evenement> findByParticipantsContaining(Utilisateur participant);

    List<Evenement> findByDateAfter(LocalDateTime date);

    List<Evenement> findByNomContainingIgnoreCase(String nom);
}