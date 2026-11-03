package com.tailfriends.breeding.repository;

import com.tailfriends.breeding.domain.Clutch;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClutchRepository extends JpaRepository<Clutch, Long> {

    /**
     * 여러 번식 기록의 산란 기록. 산란일 오래된 순, 같으면 먼저 넣은 것 먼저.
     */
    List<Clutch> findAllByBreedingIdInOrderByLaidDateAscIdAsc(Collection<Long> breedingIds);

    /**
     * 산란 기록을 통째로 바꿀 때 한 문장으로 지운다.
     */
    @Modifying(flushAutomatically = true)
    @Query("delete from Clutch c where c.breeding.id = :breedingId")
    void deleteAllByBreedingId(@Param("breedingId") Long breedingId);
}
