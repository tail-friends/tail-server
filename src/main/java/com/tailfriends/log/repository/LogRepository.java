package com.tailfriends.log.repository;

import com.tailfriends.log.domain.Log;
import com.tailfriends.log.domain.LogType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LogRepository extends JpaRepository<Log, Long> {

    Optional<Log> findByIdAndUserId(Long id, Long userId);

    Optional<Log> findByUserIdAndClientId(Long userId, UUID clientId);

    /**
     * 기록일 최신순, 같은 날이면 나중에 저장한 순. 첫 페이지.
     */
    @Query("""
            select l from Log l
            where l.animal.id = :animalId and l.type in :types
            order by l.logDate desc, l.id desc
            """)
    List<Log> findFirstPage(@Param("animalId") Long animalId,
                            @Param("types") Collection<LogType> types,
                            Pageable pageable);

    /**
     * 커서 기록(cursorDate, cursorId) 다음부터. 정렬은 첫 페이지와 같다.
     */
    @Query("""
            select l from Log l
            where l.animal.id = :animalId and l.type in :types
              and (l.logDate < :cursorDate or (l.logDate = :cursorDate and l.id < :cursorId))
            order by l.logDate desc, l.id desc
            """)
    List<Log> findPageAfter(@Param("animalId") Long animalId,
                            @Param("types") Collection<LogType> types,
                            @Param("cursorDate") LocalDate cursorDate,
                            @Param("cursorId") Long cursorId,
                            Pageable pageable);
}
