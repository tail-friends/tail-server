package com.tailfriends.breeding.repository;

import com.tailfriends.breeding.domain.Breeding;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BreedingRepository extends JpaRepository<Breeding, Long> {

    Optional<Breeding> findByIdAndUserId(Long id, Long userId);
}
