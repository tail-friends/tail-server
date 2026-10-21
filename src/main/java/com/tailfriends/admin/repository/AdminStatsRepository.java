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
}
