package com.tailfriends.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.tailfriends.user.domain.Role;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class JwtProviderTest {

    private static final String SECRET = "test-jwt-secret-that-is-at-least-32-bytes-long";
    private static final Instant NOW = Instant.parse("2026-10-19T00:00:00Z");
    private static final Duration ACCESS_TOKEN_EXPIRATION = Duration.ofMinutes(30);

    private final JwtProvider jwtProvider = providerAt(NOW, SECRET);

    @Test
    void 발급한_액세스_토큰에서_사용자_ID와_권한을_꺼낸다() {
        String token = jwtProvider.createAccessToken(1L, Role.ADMIN);

        assertThat(jwtProvider.parseAccessToken(token))
                .contains(new AccessTokenClaims(1L, Role.ADMIN));
    }

    @Test
    void 만료된_토큰은_빈_값을_돌려준다() {
        String token = jwtProvider.createAccessToken(1L, Role.KEEPER);
        JwtProvider later = providerAt(NOW.plus(ACCESS_TOKEN_EXPIRATION).plusSeconds(1), SECRET);

        assertThat(later.parseAccessToken(token)).isEmpty();
    }

    @Test
    void 다른_키로_서명한_토큰은_빈_값을_돌려준다() {
        String token = providerAt(NOW, "another-jwt-secret-that-is-at-least-32-bytes")
                .createAccessToken(1L, Role.KEEPER);

        assertThat(jwtProvider.parseAccessToken(token)).isEmpty();
    }

    @Test
    void 형식이_잘못된_토큰은_빈_값을_돌려준다() {
        assertThat(jwtProvider.parseAccessToken("not-a-jwt")).isEmpty();
        assertThat(jwtProvider.parseAccessToken("")).isEmpty();
    }

    private static JwtProvider providerAt(Instant now, String secret) {
        JwtProperties properties = new JwtProperties(secret, ACCESS_TOKEN_EXPIRATION, Duration.ofDays(14));
        return new JwtProvider(properties, Clock.fixed(now, ZoneOffset.UTC));
    }
}
