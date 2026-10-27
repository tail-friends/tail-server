package com.tailfriends.auth.repository;

import com.tailfriends.auth.domain.RefreshToken;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
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

    /**
     * 아직 쓰지 않은 토큰이면 쓴 시각을 남기고 1을 돌려준다. 같은 토큰으로 동시에 재발급하면 한 요청만 1을 받는다.
     */
    @Modifying(flushAutomatically = true)
    @Query("update RefreshToken r set r.usedAt = :now, r.updatedAt = :now where r.id = :id and r.usedAt is null")
    int markUsed(@Param("id") Long id, @Param("now") Instant now);

    /**
     * 토큰 묶음을 모두 지운다. 쓴 토큰이 다시 오면 그 로그인을 끊는다.
     */
    @Modifying(flushAutomatically = true)
    @Query("delete from RefreshToken r where r.familyId = :familyId")
    int deleteByFamilyId(@Param("familyId") UUID familyId);

    /**
     * 내 토큰이면 그 토큰의 묶음을 모두 지운다. 없거나 남의 토큰이면 0.
     */
    @Modifying(flushAutomatically = true)
    @Query("""
            delete from RefreshToken r where r.familyId in (
                select t.familyId from RefreshToken t where t.tokenHash = :tokenHash and t.user.id = :userId)
            """)
    int deleteFamilyByTokenHashAndUserId(@Param("tokenHash") String tokenHash, @Param("userId") Long userId);
}
