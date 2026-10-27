package com.tailfriends.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class RefreshTokenTest {

    private static final Instant USED_AT = Instant.parse("2026-10-27T10:00:00Z");
    private static final Duration GRACE = Duration.ofSeconds(10);

    @Test
    void 쓰지_않은_토큰은_유예_시간_안이_아니다() {
        RefreshToken token = token(null);

        assertThat(token.isUsed()).isFalse();
        assertThat(token.isUsedWithin(GRACE, USED_AT)).isFalse();
    }

    @Test
    void 쓴_지_10초가_안_됐으면_유예_시간_안이다() {
        RefreshToken token = token(USED_AT);

        assertThat(token.isUsed()).isTrue();
        assertThat(token.isUsedWithin(GRACE, USED_AT.plusSeconds(9))).isTrue();
    }

    @Test
    void 쓴_지_딱_10초가_지나면_유예_시간이_끝난다() {
        RefreshToken token = token(USED_AT);

        assertThat(token.isUsedWithin(GRACE, USED_AT.plusSeconds(10))).isFalse();
        assertThat(token.isUsedWithin(GRACE, USED_AT.plusSeconds(60))).isFalse();
    }

    private static RefreshToken token(Instant usedAt) {
        RefreshToken token = RefreshToken.builder()
                .tokenHash(RefreshToken.hash("raw"))
                .expiresAt(USED_AT.plus(Duration.ofDays(14)))
                .familyId(UUID.randomUUID())
                .build();
        ReflectionTestUtils.setField(token, "usedAt", usedAt);
        return token;
    }
}
