package com.tailfriends.auth.repository;

import com.tailfriends.auth.domain.RefreshToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * 지운 행 수를 돌려준다. 같은 토큰으로 동시에 재발급하면 한 요청만 1을 받는다.
     */
    @Modifying(flushAutomatically = true)
    @Query("delete from RefreshToken r where r.id = :id")
    int deleteByIdReturningCount(@Param("id") Long id);

    @Modifying(flushAutomatically = true)
    @Query("delete from RefreshToken r where r.tokenHash = :tokenHash and r.user.id = :userId")
    int deleteByTokenHashAndUserId(@Param("tokenHash") String tokenHash, @Param("userId") Long userId);
}
