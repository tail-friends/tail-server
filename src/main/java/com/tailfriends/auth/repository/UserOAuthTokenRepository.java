package com.tailfriends.auth.repository;

import com.tailfriends.auth.domain.UserOAuthToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserOAuthTokenRepository extends JpaRepository<UserOAuthToken, Long> {

    Optional<UserOAuthToken> findByUserId(Long userId);

    /**
     * 암호화한 토큰 값만 읽는다. 엔티티를 영속성 컨텍스트에 올린 채로 사용자를 지우면 flush할 때
     * 지워진 사용자를 가리킨다는 TransientObjectException이 나서, 탈퇴할 때는 이것을 쓴다.
     */
    @Query("select t.refreshToken from UserOAuthToken t where t.user.id = :userId")
    Optional<String> findRefreshTokenByUserId(@Param("userId") Long userId);
}
