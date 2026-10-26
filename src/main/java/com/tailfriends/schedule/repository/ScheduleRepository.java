package com.tailfriends.schedule.repository;

import com.tailfriends.schedule.domain.Schedule;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    Optional<Schedule> findByIdAndUserId(Long id, Long userId);

    /**
     * 다음 일정일 순, 끝난 일정(nextDate null)은 맨 뒤.
     */
    @Query("""
            select s from Schedule s
            where s.user.id = :userId
            order by case when s.nextDate is null then 1 else 0 end, s.nextDate, s.id
            """)
    List<Schedule> findAllOfUser(@Param("userId") Long userId);

    /**
     * 그 개체의 일정과 공통 일정(animal null). 정렬은 findAllOfUser와 같다.
     */
    @Query("""
            select s from Schedule s
            where s.user.id = :userId and (s.animal is null or s.animal.id = :animalId)
            order by case when s.nextDate is null then 1 else 0 end, s.nextDate, s.id
            """)
    List<Schedule> findAllOfAnimal(@Param("userId") Long userId, @Param("animalId") Long animalId);
}
