package com.tailfriends.point.repository;

import com.tailfriends.point.domain.PointHistory;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {

    /**
     * 기록 1건의 적립을 넣고 넣은 행 수(1 또는 0)를 돌려준다.
     * 같은 개체·이유·날짜가 이미 있으면 유니크 인덱스에 막혀 0이다. 동시에 들어온 같은 적립도 한 건만 들어간다.
     * 사용자 행을 잠그지 않는다: 기록 저장이 사용자 행에 외래 키 잠금을 이미 걸어서, 여기서 FOR UPDATE로 잠그면
     * 같은 사용자의 동시 기록끼리 교착 상태가 된다.
     */
    @Modifying
    @Query(value = """
            insert into point_histories (user_id, animal_id, log_id, amount, reason, earned_date, created_at, updated_at)
            values (:userId, :animalId, :logId, :amount, :reason, :earnedDate, now(), now())
            on conflict (animal_id, reason, earned_date) do nothing
            """, nativeQuery = true)
    int insertEarning(@Param("userId") Long userId, @Param("animalId") Long animalId, @Param("logId") Long logId,
                      @Param("amount") int amount, @Param("reason") String reason,
                      @Param("earnedDate") LocalDate earnedDate);

    /**
     * 최신순. before보다 작은 ID만 돌려준다(커서).
     */
    @Query("select h from PointHistory h where h.user.id = :userId and h.id < :before order by h.id desc")
    List<PointHistory> findPage(@Param("userId") Long userId, @Param("before") long before, Pageable limit);
}
