package com.tailfriends.photo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.photo.ai.AiProperties;
import com.tailfriends.photo.domain.AiKind;
import com.tailfriends.photo.domain.AiUsage;
import com.tailfriends.photo.repository.AiUsageRepository;
import com.tailfriends.user.domain.User;
import com.tailfriends.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AiUsageLimiterTest {

    // 2026-10-20 16:00 UTC = 2026-10-21 01:00 KST. 오늘(KST)은 2026-10-20 15:00 UTC부터
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-20T16:00:00Z"), ZoneOffset.UTC);
    private static final Instant START_OF_TODAY = Instant.parse("2026-10-20T15:00:00Z");
    private static final Long USER_ID = 1L;

    private final AiUsageRepository aiUsageRepository = mock(AiUsageRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final AiUsageLimiter limiter = new AiUsageLimiter(aiUsageRepository, userRepository,
            new AiProperties("gemini-test", 5, "key", 10), CLOCK);
    private final User user = mock(User.class);

    @BeforeEach
    void setUp() {
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));
        given(aiUsageRepository.save(any())).willAnswer(invocation -> {
            AiUsage usage = invocation.getArgument(0);
            ReflectionTestUtils.setField(usage, "id", 77L);
            return usage;
        });
    }

    @Test
    void 성공_4번_시도_9번이면_성공으로_미리_기록하고_ID를_돌려준다() {
        given(aiUsageRepository.countByUserIdAndSuccessTrueAndCreatedAtGreaterThanEqual(USER_ID, START_OF_TODAY))
                .willReturn(4L);
        given(aiUsageRepository.countByUserIdAndCreatedAtGreaterThanEqual(USER_ID, START_OF_TODAY)).willReturn(9L);

        assertThat(limiter.reserve(USER_ID, AiKind.CHARACTER)).isEqualTo(77L);
        verify(aiUsageRepository).save(argThat(usage -> usage.isSuccess() && usage.getUser() == user));
    }

    @Test
    void 오늘_성공이_5번이면_AI_LIMIT_EXCEEDED() {
        given(aiUsageRepository.countByUserIdAndSuccessTrueAndCreatedAtGreaterThanEqual(USER_ID, START_OF_TODAY))
                .willReturn(5L);

        assertError(ErrorCode.AI_LIMIT_EXCEEDED);
        verify(aiUsageRepository, never()).save(any());
    }

    @Test
    void 성공이_적어도_실패를_포함한_시도가_10번이면_AI_LIMIT_EXCEEDED() {
        given(aiUsageRepository.countByUserIdAndSuccessTrueAndCreatedAtGreaterThanEqual(USER_ID, START_OF_TODAY))
                .willReturn(1L);
        given(aiUsageRepository.countByUserIdAndCreatedAtGreaterThanEqual(USER_ID, START_OF_TODAY)).willReturn(10L);

        assertError(ErrorCode.AI_LIMIT_EXCEEDED);
        verify(aiUsageRepository, never()).save(any());
    }

    @Test
    void 사용자가_없으면_UNAUTHORIZED() {
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.empty());

        assertError(ErrorCode.UNAUTHORIZED);
    }

    @Test
    void 풀면_실패로_바뀐다() {
        AiUsage usage = AiUsage.of(user, AiKind.CHARACTER, true);
        given(aiUsageRepository.findById(77L)).willReturn(Optional.of(usage));

        limiter.release(77L);

        assertThat(usage.isSuccess()).isFalse();
    }

    private void assertError(ErrorCode code) {
        assertThatThrownBy(() -> limiter.reserve(USER_ID, AiKind.CHARACTER))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(code);
    }
}
