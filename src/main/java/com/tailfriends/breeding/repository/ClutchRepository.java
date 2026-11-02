package com.tailfriends.breeding.repository;

import com.tailfriends.breeding.domain.Clutch;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClutchRepository extends JpaRepository<Clutch, Long> {

    /**
     * 여러 번식 기록의 산란 기록. 산란일 오래된 순, 같으면 먼저 넣은 것 먼저.
     */
    List<Clutch> findAllByBreedingIdInOrderByLaidDateAscIdAsc(Collection<Long> breedingIds);
}
