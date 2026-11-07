package com.tailfriends.health.repository;

import com.tailfriends.health.domain.HealthCheck;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HealthCheckRepository extends JpaRepository<HealthCheck, Long> {

    /**
     * 최신순. before보다 작은 ID만 (첫 페이지는 Long.MAX_VALUE). 썸네일을 위해 사진을 함께 읽는다.
     */
    @Query("""
            select c from HealthCheck c left join fetch c.photo
            where c.animal.id = :animalId and c.id < :before
            order by c.id desc
            """)
    List<HealthCheck> findPage(@Param("animalId") Long animalId, @Param("before") long before, Pageable pageable);

    /**
     * 꼬리 두께를 비교할 직전 체크.
     */
    Optional<HealthCheck> findFirstByAnimalIdAndPhotoIsNotNullOrderByIdDesc(Long animalId);

    List<HealthCheck> findAllByPhotoIdIn(Collection<Long> photoIds);
}
