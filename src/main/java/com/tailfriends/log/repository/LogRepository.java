package com.tailfriends.log.repository;

import com.tailfriends.log.domain.Log;
import com.tailfriends.log.domain.LogType;
import com.tailfriends.log.domain.Stool;
import java.math.BigDecimal;
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
     * 마지막 급여일. "안 먹음" 기록은 빼고 본다. (기능명세서 4-1)
     */
    @Query("""
            select max(l.logDate) from Log l
            where l.animal.id = :animalId
              and l.type = com.tailfriends.log.domain.LogType.FEED and l.refused = false
            """)
    Optional<LocalDate> findLastFedDate(@Param("animalId") Long animalId);

    /**
     * 최근 급여 기록의 "안 먹음" 여부, 최신순. 개수는 pageable로 정한다.
     */
    @Query("""
            select l.refused from Log l
            where l.animal.id = :animalId and l.type = com.tailfriends.log.domain.LogType.FEED
            order by l.logDate desc, l.id desc
            """)
    List<Boolean> findLatestRefused(@Param("animalId") Long animalId, Pageable pageable);

    Optional<Log> findFirstByAnimalIdAndTypeOrderByLogDateDescIdDesc(Long animalId, LogType type);

    /**
     * 기간 안 최고 체중. from, to 모두 포함.
     */
    @Query("""
            select max(l.weightG) from Log l
            where l.animal.id = :animalId and l.type = com.tailfriends.log.domain.LogType.WEIGHT
              and l.logDate between :from and :to
            """)
    Optional<BigDecimal> findHighestWeight(@Param("animalId") Long animalId,
                                           @Param("from") LocalDate from,
                                           @Param("to") LocalDate to);

    /**
     * 배변이 있었던(정상·묽음) 최근 청소 기록의 배변 상태, 최신순.
     */
    @Query("""
            select l.stool from Log l
            where l.animal.id = :animalId and l.type = com.tailfriends.log.domain.LogType.CLEAN
              and l.stool in (com.tailfriends.log.domain.Stool.NORMAL, com.tailfriends.log.domain.Stool.LOOSE)
            order by l.logDate desc, l.id desc
            """)
    List<Stool> findLatestStools(@Param("animalId") Long animalId, Pageable pageable);

    /**
     * 여러 개체의 마지막 급여일을 한 번에 조회한다. 급여 기록이 없는 개체는 결과에 없다.
     */
    @Query("""
            select new com.tailfriends.log.repository.LastFedDate(l.animal.id, max(l.logDate)) from Log l
            where l.animal.id in :animalIds
              and l.type = com.tailfriends.log.domain.LogType.FEED and l.refused = false
            group by l.animal.id
            """)
    List<LastFedDate> findLastFedDates(@Param("animalIds") Collection<Long> animalIds);

    /**
     * 기간 안 체중 기록, 날짜 오름차순. from, to 모두 포함.
     */
    @Query("""
            select new com.tailfriends.log.repository.WeightPoint(l.logDate, l.weightG) from Log l
            where l.animal.id = :animalId and l.type = com.tailfriends.log.domain.LogType.WEIGHT
              and l.logDate between :from and :to
            order by l.logDate asc, l.id asc
            """)
    List<WeightPoint> findWeights(@Param("animalId") Long animalId,
                                  @Param("from") LocalDate from,
                                  @Param("to") LocalDate to);

    /**
     * 최근 탈피 기록(블루 단계 포함), 최신순. 개수는 pageable로 정한다.
     */
    @Query("""
            select new com.tailfriends.log.repository.ShedEntry(l.logDate, l.shedStage, l.shedComplete) from Log l
            where l.animal.id = :animalId and l.type = com.tailfriends.log.domain.LogType.SHED
            order by l.logDate desc, l.id desc
            """)
    List<ShedEntry> findLatestSheds(@Param("animalId") Long animalId, Pageable pageable);

    /**
     * 최근 체중(g), 최신순. 개수는 pageable로 정한다.
     */
    @Query("""
            select l.weightG from Log l
            where l.animal.id = :animalId and l.type = com.tailfriends.log.domain.LogType.WEIGHT
            order by l.logDate desc, l.id desc
            """)
    List<BigDecimal> findLatestWeights(@Param("animalId") Long animalId, Pageable pageable);

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
