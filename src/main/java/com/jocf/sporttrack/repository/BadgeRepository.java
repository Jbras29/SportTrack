package com.jocf.sporttrack.repository;


import com.jocf.sporttrack.model.Badge;
import com.jocf.sporttrack.model.Utilisateur;
import org.springframework.data.domain.Example;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BadgeRepository extends JpaRepository<Utilisateur, Long> {

    List<Badge> findByKmNecessaireLessThanEqual(int distanceEnKm);

}
