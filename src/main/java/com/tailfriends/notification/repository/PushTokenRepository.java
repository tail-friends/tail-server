package com.tailfriends.notification.repository;

import com.tailfriends.notification.domain.PushToken;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PushTokenRepository extends JpaRepository<PushToken, Long> {

    Optional<PushToken> findByToken(String token);

    /**
     * 알림 시간(설정이 없으면 09:00)이 [from, to) 안이고, 알림 수신에 동의했고(가장 최근 동의 기록), 토큰이 있는 사용자.
     */
    @Query(value = """
            SELECT u.id FROM users u
            LEFT JOIN user_settings s ON s.user_id = u.id
            WHERE COALESCE(s.notify_time, TIME '09:00') >= :from
              AND COALESCE(s.notify_time, TIME '09:00') < :to
              AND EXISTS (SELECT 1 FROM push_tokens t WHERE t.user_id = u.id)
              AND (SELECT c.agreed FROM consents c
                   WHERE c.user_id = u.id AND c.type = 'NOTIFICATION'
                   ORDER BY c.id DESC LIMIT 1) IS TRUE
            ORDER BY u.id
            """, nativeQuery = true)
    List<Long> findUserIdsToNotify(@Param("from") LocalTime from, @Param("to") LocalTime to);

    List<PushToken> findAllByUserIdIn(Collection<Long> userIds);

    @Modifying
    @Query("delete from PushToken t where t.user.id = :userId and t.token = :token")
    int deleteByUserIdAndToken(@Param("userId") Long userId, @Param("token") String token);

    @Modifying
    @Query("delete from PushToken t where t.token in :tokens")
    int deleteByTokenIn(@Param("tokens") Collection<String> tokens);
}
