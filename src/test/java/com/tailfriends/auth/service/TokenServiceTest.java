package com.tailfriends.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.tailfriends.auth.domain.RefreshToken;
import com.tailfriends.auth.dto.TokenResponse;
import com.tailfriends.auth.repository.RefreshTokenRepository;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.security.AccessTokenClaims;
import com.tailfriends.global.security.JwtProperties;
import com.tailfriends.global.security.JwtProvider;
import com.tailfriends.user.domain.Provider;
import com.tailfriends.user.domain.Role;
import com.tailfriends.user.domain.User;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class TokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-19T00:00:00Z");

    private final JwtProperties jwtProperties = new JwtProperties(
            "test-jwt-secret-that-is-at-least-32-bytes-long", Duration.ofMinutes(30), Duration.ofDays(14));
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final JwtProvider jwtProvider = new JwtProvider(jwtProperties, clock);
    private final RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
    private final TokenService tokenService = new TokenService(jwtProvider, jwtProperties, refreshTokenRepository, clock);

    @Test
    void 액세스_토큰에는_사용자_ID와_권한이_들어간다() {
        given(refreshTokenRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        TokenResponse response = tokenService.issue(user(3L));

        assertThat(jwtProvider.parseAccessToken(response.accessToken()))
                .contains(new AccessTokenClaims(3L, Role.KEEPER));
    }

    @Test
    void 리프레시_토큰은_원문_대신_해시를_14일_만료로_저장한다() {
        given(refreshTokenRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        TokenResponse response = tokenService.issue(user(3L));

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken saved = captor.getValue();
        assertThat(saved.getTokenHash())
                .hasSize(64)
                .isNotEqualTo(response.refreshToken())
                .isEqualTo(RefreshToken.hash(response.refreshToken()));
        assertThat(saved.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofDays(14)));
    }

    @Test
    void 발급할_때마다_다른_리프레시_토큰을_만든다() {
        given(refreshTokenRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
        User user = user(3L);

        assertThat(tokenService.issue(user).refreshToken())
                .isNotEqualTo(tokenService.issue(user).refreshToken());
    }

    @Test
    void 재발급하면_쓴_리프레시_토큰을_지우고_새_토큰을_발급한다() {
        RefreshToken old = savedRefreshToken("old-refresh", NOW.plus(Duration.ofDays(1)));
        given(refreshTokenRepository.findByTokenHash(RefreshToken.hash("old-refresh"))).willReturn(Optional.of(old));
        given(refreshTokenRepository.deleteByIdReturningCount(10L)).willReturn(1);
        given(refreshTokenRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        TokenResponse response = tokenService.reissue("old-refresh");

        verify(refreshTokenRepository).deleteByIdReturningCount(10L);
        assertThat(response.refreshToken()).isNotEqualTo("old-refresh");
        assertThat(jwtProvider.parseAccessToken(response.accessToken()))
                .contains(new AccessTokenClaims(3L, Role.KEEPER));
    }

    @Test
    void 저장되지_않은_리프레시_토큰이면_재발급하지_않는다() {
        given(refreshTokenRepository.findByTokenHash(any())).willReturn(Optional.empty());

        assertInvalidRefreshToken("unknown");
    }

    @Test
    void 만료된_리프레시_토큰이면_재발급하지_않는다() {
        RefreshToken expired = savedRefreshToken("expired", NOW);
        given(refreshTokenRepository.findByTokenHash(RefreshToken.hash("expired"))).willReturn(Optional.of(expired));

        assertInvalidRefreshToken("expired");
        verify(refreshTokenRepository, never()).deleteByIdReturningCount(any());
    }

    @Test
    void 동시에_먼저_쓰인_리프레시_토큰이면_재발급하지_않는다() {
        RefreshToken used = savedRefreshToken("used", NOW.plus(Duration.ofDays(1)));
        given(refreshTokenRepository.findByTokenHash(RefreshToken.hash("used"))).willReturn(Optional.of(used));
        given(refreshTokenRepository.deleteByIdReturningCount(10L)).willReturn(0);

        assertInvalidRefreshToken("used");
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void 로그아웃하면_내_리프레시_토큰의_해시로_지운다() {
        tokenService.revoke(3L, "my-refresh");

        verify(refreshTokenRepository).deleteByTokenHashAndUserId(RefreshToken.hash("my-refresh"), 3L);
    }

    private void assertInvalidRefreshToken(String refreshToken) {
        assertThatThrownBy(() -> tokenService.reissue(refreshToken))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    private static RefreshToken savedRefreshToken(String rawToken, Instant expiresAt) {
        RefreshToken token = RefreshToken.builder()
                .user(user(3L))
                .tokenHash(RefreshToken.hash(rawToken))
                .expiresAt(expiresAt)
                .build();
        ReflectionTestUtils.setField(token, "id", 10L);
        return token;
    }

    private static User user(Long id) {
        User user = User.builder()
                .provider(Provider.KAKAO)
                .providerId("kakao-" + id)
                .nickname("사용자")
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
