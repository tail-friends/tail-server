package com.tailfriends.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.tailfriends.auth.domain.RefreshToken;
import com.tailfriends.auth.dto.TokenResponse;
import com.tailfriends.auth.repository.RefreshTokenRepository;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.security.AccessTokenClaims;
import com.tailfriends.global.security.JwtProperties;
import com.tailfriends.global.security.JwtProvider;
import com.tailfriends.global.support.CurrentUserStub;
import com.tailfriends.user.domain.Provider;
import com.tailfriends.user.domain.Role;
import com.tailfriends.user.domain.User;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class TokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-19T00:00:00Z");
    private static final UUID FAMILY_ID = UUID.fromString("5a0e0000-0000-4000-8000-000000000001");

    private final JwtProperties jwtProperties = new JwtProperties(
            "test-jwt-secret-that-is-at-least-32-bytes-long", Duration.ofMinutes(30), Duration.ofDays(14));
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final JwtProvider jwtProvider = new JwtProvider(jwtProperties, clock);
    private final RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
    private final TokenService tokenService = new TokenService(CurrentUserStub.of(3L), jwtProvider, jwtProperties, refreshTokenRepository, clock);

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
    void 재발급하면_쓴_리프레시_토큰에_쓴_시각을_남기고_새_토큰을_발급한다() {
        RefreshToken old = savedRefreshToken("old-refresh", NOW.plus(Duration.ofDays(1)));
        given(refreshTokenRepository.findByTokenHash(RefreshToken.hash("old-refresh"))).willReturn(Optional.of(old));
        given(refreshTokenRepository.markUsed(10L, NOW)).willReturn(1);
        given(refreshTokenRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        TokenResponse response = tokenService.reissue("old-refresh");

        verify(refreshTokenRepository).markUsed(10L, NOW);
        assertThat(response.refreshToken()).isNotEqualTo("old-refresh");
        assertThat(jwtProvider.parseAccessToken(response.accessToken()))
                .contains(new AccessTokenClaims(3L, Role.KEEPER));
    }

    @Test
    void 로그인할_때마다_새_묶음으로_발급하고_재발급한_토큰은_묶음을_이어받는다() {
        given(refreshTokenRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
        RefreshToken old = savedRefreshToken("old-refresh", NOW.plus(Duration.ofDays(1)));
        given(refreshTokenRepository.findByTokenHash(RefreshToken.hash("old-refresh"))).willReturn(Optional.of(old));
        given(refreshTokenRepository.markUsed(10L, NOW)).willReturn(1);

        tokenService.issue(user(3L));
        tokenService.issue(user(3L));
        tokenService.reissue("old-refresh");

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, times(3)).save(captor.capture());
        UUID first = captor.getAllValues().get(0).getFamilyId();
        UUID second = captor.getAllValues().get(1).getFamilyId();
        assertThat(first).isNotNull().isNotEqualTo(second);
        assertThat(captor.getAllValues().get(2).getFamilyId()).isEqualTo(FAMILY_ID);
    }

    @Test
    void 쓴_지_10초가_지난_토큰이_다시_오면_그_묶음을_모두_지운다() {
        RefreshToken reused = savedRefreshToken("reused", NOW.plus(Duration.ofDays(1)));
        ReflectionTestUtils.setField(reused, "usedAt", NOW.minusSeconds(10));
        given(refreshTokenRepository.findByTokenHash(RefreshToken.hash("reused"))).willReturn(Optional.of(reused));

        assertInvalidRefreshToken("reused");
        verify(refreshTokenRepository).deleteByFamilyId(FAMILY_ID);
        verify(refreshTokenRepository, never()).markUsed(any(), any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void 쓴_지_10초_안에_다시_오면_재발급만_막고_묶음은_지우지_않는다() {
        RefreshToken retried = savedRefreshToken("retried", NOW.plus(Duration.ofDays(1)));
        ReflectionTestUtils.setField(retried, "usedAt", NOW.minusSeconds(9));
        given(refreshTokenRepository.findByTokenHash(RefreshToken.hash("retried"))).willReturn(Optional.of(retried));

        assertInvalidRefreshToken("retried");
        verify(refreshTokenRepository, never()).deleteByFamilyId(any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void 만료된_토큰은_쓴_적이_있어도_묶음을_지우지_않는다() {
        RefreshToken expired = savedRefreshToken("expired-used", NOW);
        ReflectionTestUtils.setField(expired, "usedAt", NOW.minus(Duration.ofDays(1)));
        given(refreshTokenRepository.findByTokenHash(RefreshToken.hash("expired-used"))).willReturn(Optional.of(expired));

        assertInvalidRefreshToken("expired-used");
        verify(refreshTokenRepository, never()).deleteByFamilyId(any());
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
        verify(refreshTokenRepository, never()).markUsed(any(), any());
    }

    @Test
    void 동시에_먼저_쓰인_리프레시_토큰이면_재발급하지_않는다() {
        RefreshToken used = savedRefreshToken("used", NOW.plus(Duration.ofDays(1)));
        given(refreshTokenRepository.findByTokenHash(RefreshToken.hash("used"))).willReturn(Optional.of(used));
        given(refreshTokenRepository.markUsed(10L, NOW)).willReturn(0);

        assertInvalidRefreshToken("used");
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void 로그아웃하면_내_리프레시_토큰의_해시로_그_묶음을_지운다() {
        tokenService.revoke("my-refresh");

        verify(refreshTokenRepository).deleteFamilyByTokenHashAndUserId(RefreshToken.hash("my-refresh"), 3L);
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
                .familyId(FAMILY_ID)
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
