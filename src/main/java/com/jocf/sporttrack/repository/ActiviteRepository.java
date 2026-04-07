package com.jocf.sporttrack.repository;

import com.jocf.sporttrack.model.Activite;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActiviteRepository extends JpaRepository<Activite, Long> {

}