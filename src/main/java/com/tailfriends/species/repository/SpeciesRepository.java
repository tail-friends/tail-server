package com.tailfriends.species.repository;

import com.tailfriends.species.domain.Species;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpeciesRepository extends JpaRepository<Species, Long> {

    List<Species> findAllByHiddenFalseOrderByIdAsc();

    List<Species> findAllByOrderByIdAsc();

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}
