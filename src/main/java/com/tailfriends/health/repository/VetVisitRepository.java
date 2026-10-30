package com.tailfriends.health.repository;

import com.tailfriends.health.domain.VetVisit;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VetVisitRepository extends JpaRepository<VetVisit, Long> {

    /**
     * 방문일 최신순, 같으면 나중에 적은 것 먼저.
     */
    List<VetVisit> findAllByAnimalIdOrderByVisitDateDescIdDesc(Long animalId);

    /**
     * 개체 주인이 userId인 진료 기록만. 남의 기록이면 비어 있다.
     */
    @Query("select v from VetVisit v where v.id = :id and v.animal.user.id = :userId")
    Optional<VetVisit> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}
