package com.tailfriends.photo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.tailfriends.global.support.CurrentUserStub;
import com.tailfriends.photo.ai.AiProperties;
import com.tailfriends.photo.dto.AiUsageResponse;
import com.tailfriends.photo.repository.AiUsageRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class AiUsageServiceTest {

    // 2026-10-20 16:00 UTC = 2026-10-21 01:00 KST. 오늘(KST)은 2026-10-20 15:00 UTC부터
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-20T16:00:00Z"), ZoneOffset.UTC);
    private static final Instant START_OF_TODAY = Instant.parse("2026-10-20T15:00:00Z");
    private static final Long USER_ID = 1L;

    private final AiUsageRepository aiUsageRepository = mock(AiUsageRepository.class);
    private final AiUsageService service = new AiUsageService(CurrentUserStub.of(USER_ID), aiUsageRepository,
            new AiProperties("gemini-test", 5, "key", 10), CLOCK);

    @Test
    void 오늘_KST_0시부터_성공한_분석_수를_used로_돌려준다() {
        givenCounts(3, 4);

        AiUsageResponse usage = service.getTodayUsage();

        assertThat(usage.limit()).isEqualTo(5);
        assertThat(usage.used()).isEqualTo(3);
    }

    @Test
    void 다시_채워지는_시각은_다음_날_KST_0시다() {
        givenCounts(0, 0);

        assertThat(service.getTodayUsage().resetsAt().toInstant())
                .isEqualTo(Instant.parse("2026-10-21T15:00:00Z"));
    }

    @Test
    void 성공이_적어도_시도_한도를_다_썼으면_used는_limit이다() {
        givenCounts(1, 10);

        assertThat(service.getTodayUsage().used()).isEqualTo(5);
    }

    @Test
    void 한도를_줄여도_used는_limit을_넘지_않는다() {
        givenCounts(7, 7);

        assertThat(service.getTodayUsage().used()).isEqualTo(5);
    }

    private void givenCounts(long succeeded, long attempted) {
        given(aiUsageRepository.countByUserIdAndSuccessTrueAndCreatedAtGreaterThanEqual(USER_ID, START_OF_TODAY))
                .willReturn(succeeded);
        given(aiUsageRepository.countByUserIdAndCreatedAtGreaterThanEqual(USER_ID, START_OF_TODAY))
                .willReturn(attempted);
    }
}
