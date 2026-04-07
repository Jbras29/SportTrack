package com.jocf.sporttrack.repository;


import com.jocf.sporttrack.model.Badge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BadgeRepository extends JpaRepository<Badge, Long> {

    List<Badge> findByKmNecessaireLessThanEqual(int distanceEnKm);

}
