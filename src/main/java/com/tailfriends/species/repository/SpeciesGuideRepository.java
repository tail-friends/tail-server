package com.tailfriends.species.repository;

import com.tailfriends.species.domain.SpeciesGuide;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpeciesGuideRepository extends JpaRepository<SpeciesGuide, Long> {

    List<SpeciesGuide> findAllBySpeciesId(Long speciesId);
}
