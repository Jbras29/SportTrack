package com.jocf.sporttrack.repository;

import com.jocf.sporttrack.model.PrefSportive;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PrefSportiveRepository extends JpaRepository<PrefSportive, Long> {

    Optional<PrefSportive> findByNom(String nom);
}
