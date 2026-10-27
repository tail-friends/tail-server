package com.tailfriends.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.tailfriends.auth.domain.RefreshToken;
import com.tailfriends.auth.service.RefreshTokenCleaner;
import com.tailfriends.auth.service.TokenService;
import com.tailfriends.global.support.IntegrationTestSupport;
import com.tailfriends.user.domain.Provider;
import com.tailfriends.user.domain.User;
import com.tailfriends.user.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class RefreshTokenCleanerIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private RefreshTokenCleaner refreshTokenCleaner;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void 만료된_토큰만_지우고_쓴_토큰도_만료_전이면_남긴다() {
        User user = saveUser();
        String expired = tokenService.issue(user).refreshToken();
        String used = tokenService.issue(user).refreshToken();
        tokenService.reissue(used);
        String active = tokenService.issue(user).refreshToken();
        // 발급 시각을 옮길 수 없어 만료 시각을 하루 전으로 바꾼다
        jdbcTemplate.update("update refresh_tokens set expires_at = now() - interval '1 day' where token_hash = ?",
                RefreshToken.hash(expired));

        refreshTokenCleaner.deleteExpired();

        assertThat(exists(expired)).isFalse();
        assertThat(exists(used)).isTrue();
        assertThat(exists(active)).isTrue();
    }

    private boolean exists(String refreshToken) {
        Integer count = jdbcTemplate.queryForObject("select count(*) from refresh_tokens where token_hash = ?",
                Integer.class, RefreshToken.hash(refreshToken));
        return count != null && count > 0;
    }

    private User saveUser() {
        return userRepository.save(User.builder()
                .provider(Provider.KAKAO)
                .providerId(UUID.randomUUID().toString())
                .nickname("사용자")
                .build());
    }
}
