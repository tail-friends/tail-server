package com.tailfriends.health.repository;

import com.tailfriends.health.domain.AlertStatus;
import com.tailfriends.health.domain.AlertType;
import com.tailfriends.health.domain.HealthAlert;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HealthAlertRepository extends JpaRepository<HealthAlert, Long> {

    boolean existsByAnimalIdAndTypeAndStatus(Long animalId, AlertType type, AlertStatus status);

    List<HealthAlert> findAllByAnimalIdAndStatusOrderByIdDesc(Long animalId, AlertStatus status);

    @Query("select a from HealthAlert a where a.id = :id and a.animal.user.id = :userId")
    Optional<HealthAlert> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 내 전체 개체의 경고, 최신순. cursor보다 작은 ID만.
     */
    @Query("""
            select a from HealthAlert a
            where a.animal.user.id = :userId and a.status in :statuses and a.id < :cursor
            order by a.id desc
            """)
    List<HealthAlert> findPageByUser(@Param("userId") Long userId,
                                     @Param("statuses") Collection<AlertStatus> statuses,
                                     @Param("cursor") Long cursor,
                                     Pageable pageable);

    /**
     * 개체 하나의 경고, 최신순. cursor보다 작은 ID만.
     */
    @Query("""
            select a from HealthAlert a
            where a.animal.id = :animalId and a.status in :statuses and a.id < :cursor
            order by a.id desc
            """)
    List<HealthAlert> findPageByAnimal(@Param("animalId") Long animalId,
                                       @Param("statuses") Collection<AlertStatus> statuses,
                                       @Param("cursor") Long cursor,
                                       Pageable pageable);

    /**
     * 개체별 열린 경고 수. 열린 경고가 없는 개체는 결과에 없다.
     */
    @Query("""
            select new com.tailfriends.health.repository.OpenAlertCount(a.animal.id, count(a)) from HealthAlert a
            where a.animal.id in :animalIds and a.status = com.tailfriends.health.domain.AlertStatus.OPEN
            group by a.animal.id
            """)
    List<OpenAlertCount> countOpenByAnimalIds(@Param("animalIds") Collection<Long> animalIds);

    /**
     * 내 전체 개체의 열린 경고, 최신순.
     */
    @Query("""
            select new com.tailfriends.health.repository.OpenAlert(a.id, a.animal.id, a.type) from HealthAlert a
            where a.animal.user.id = :userId and a.status = com.tailfriends.health.domain.AlertStatus.OPEN
            order by a.id desc
            """)
    List<OpenAlert> findOpenByUser(@Param("userId") Long userId);
}
