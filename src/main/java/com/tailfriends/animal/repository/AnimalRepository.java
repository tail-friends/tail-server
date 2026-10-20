package com.tailfriends.animal.repository;

import com.tailfriends.animal.domain.Animal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnimalRepository extends JpaRepository<Animal, Long> {

    List<Animal> findAllByUserIdOrderByIdAsc(Long userId);

    Optional<Animal> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);
}
