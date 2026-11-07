package com.tailfriends.notification.repository;

import com.tailfriends.notification.domain.PushToken;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PushTokenRepository extends JpaRepository<PushToken, Long> {

    Optional<PushToken> findByToken(String token);

    List<PushToken> findAllByUserIdIn(Collection<Long> userIds);

    @Modifying
    @Query("delete from PushToken t where t.user.id = :userId and t.token = :token")
    int deleteByUserIdAndToken(@Param("userId") Long userId, @Param("token") String token);

    @Modifying
    @Query("delete from PushToken t where t.token in :tokens")
    int deleteByTokenIn(@Param("tokens") Collection<String> tokens);
}
