package com.tailfriends.auth.repository;

import com.tailfriends.auth.domain.UserOAuthToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserOAuthTokenRepository extends JpaRepository<UserOAuthToken, Long> {

    Optional<UserOAuthToken> findByUserId(Long userId);
}
