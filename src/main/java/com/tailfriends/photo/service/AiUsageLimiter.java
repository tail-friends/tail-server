package com.tailfriends.photo.service;

import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.util.KstDate;
import com.tailfriends.photo.ai.AiProperties;
import com.tailfriends.photo.domain.AiKind;
import com.tailfriends.photo.domain.AiUsage;
import com.tailfriends.photo.repository.AiUsageRepository;
import com.tailfriends.user.domain.User;
import com.tailfriends.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI 분석 하루 횟수를 잡아 두고 푼다. AI 호출은 몇 초 걸려 그동안 DB 잠금을 잡고 있지 않도록,
 * 짧은 트랜잭션으로 횟수를 먼저 잡고(reserve) AI를 부른 뒤 실패하면 푼다(release).
 */
@Component
@RequiredArgsConstructor
public class AiUsageLimiter {

    private final AiUsageRepository aiUsageRepository;
    private final UserRepository userRepository;
    private final AiProperties aiProperties;
    private final Clock clock;

    /**
     * 같은 사용자의 요청을 사용자 행 잠금으로 차례로 세서, 동시에 보내도 하루 횟수를 넘지 않게 한다.
     * 성공으로 미리 기록한다.
     *
     * @return 잡아 둔 기록 ID. 실패하면 release에 넘긴다
     * @throws BusinessException 오늘 성공 횟수나 시도 횟수를 다 썼으면 AI_LIMIT_EXCEEDED,
     *                           사용자가 없으면(탈퇴 등) UNAUTHORIZED
     */
    @Transactional
    public Long reserve(Long userId, AiKind kind) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        Instant startOfToday = KstDate.today(clock).atStartOfDay(KstDate.ZONE).toInstant();
        if (aiUsageRepository.countByUserIdAndSuccessTrueAndCreatedAtGreaterThanEqual(userId, startOfToday)
                >= aiProperties.dailyLimit()
                || aiUsageRepository.countByUserIdAndCreatedAtGreaterThanEqual(userId, startOfToday)
                >= aiProperties.dailyAttemptLimit()) {
            throw new BusinessException(ErrorCode.AI_LIMIT_EXCEEDED);
        }
        return aiUsageRepository.save(AiUsage.of(user, kind, true)).getId();
    }

    /**
     * AI가 답을 주지 못한 시도로 바꿔 하루 성공 횟수에서 뺀다. 시도 횟수에는 남는다.
     */
    @Transactional
    public void release(Long usageId) {
        aiUsageRepository.findById(usageId).ifPresent(AiUsage::fail);
    }
}
