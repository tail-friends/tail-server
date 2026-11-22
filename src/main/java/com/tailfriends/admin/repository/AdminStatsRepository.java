package com.tailfriends.admin.repository;

import com.tailfriends.user.domain.User;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * 운영 통계용 집계. 날짜는 한국 시간으로 묶는다. (DB 시각은 UTC)
 */
public interface AdminStatsRepository extends Repository<User, Long> {

    @Query(value = "SELECT count(*) FROM users", nativeQuery = true)
    long countUsers();

    @Query(value = "SELECT count(*) FROM animals", nativeQuery = true)
    long countAnimals();

    /**
     * 종별 등록 개체 수. 많은 순, 같으면 이름순. 개체가 없는 종은 빠지고, 숨긴 종도 개체가 있으면 들어간다.
     * 목록에 없어 직접 입력한 종은 하나로 묶어 speciesId null, 이름 "직접 입력"으로 센다.
     */
    @Query(value = """
            SELECT a.species_id AS "speciesId", COALESCE(s.name, '직접 입력') AS "name", count(*) AS "count"
            FROM animals a
            LEFT JOIN species s ON s.id = a.species_id
            GROUP BY a.species_id, s.name
            ORDER BY "count" DESC, "name"
            """, nativeQuery = true)
    List<SpeciesCount> countAnimalsBySpecies();

    @Query(value = """
            SELECT to_char(created_at AT TIME ZONE 'Asia/Seoul', 'YYYY-MM-DD') AS day, count(*) AS count
            FROM users
            WHERE created_at >= :from
            GROUP BY day
            """, nativeQuery = true)
    List<DailyCount> countNewUsersByDay(@Param("from") Instant from);

    @Query(value = """
            SELECT to_char(created_at AT TIME ZONE 'Asia/Seoul', 'YYYY-MM-DD') AS day, count(*) AS count
            FROM animals
            WHERE created_at >= :from
            GROUP BY day
            """, nativeQuery = true)
    List<DailyCount> countNewAnimalsByDay(@Param("from") Instant from);

    /**
     * AI 분석 시도 수. 모든 종류(kind)를 합친다.
     */
    @Query(value = """
            SELECT to_char(created_at AT TIME ZONE 'Asia/Seoul', 'YYYY-MM-DD') AS day, count(*) AS count
            FROM ai_usages
            WHERE created_at >= :from
            GROUP BY day
            """, nativeQuery = true)
    List<DailyCount> countAiAnalysesByDay(@Param("from") Instant from);

    /**
     * AI가 답을 주지 못한 분석 수. 파충류가 없다는 답은 성공이라 빠진다.
     */
    @Query(value = """
            SELECT to_char(created_at AT TIME ZONE 'Asia/Seoul', 'YYYY-MM-DD') AS day, count(*) AS count
            FROM ai_usages
            WHERE created_at >= :from AND NOT success
            GROUP BY day
            """, nativeQuery = true)
    List<DailyCount> countAiFailuresByDay(@Param("from") Instant from);
}
