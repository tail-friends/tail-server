package com.tailfriends.photo.service;

import com.tailfriends.global.security.CurrentUser;
import com.tailfriends.global.util.KstDate;
import com.tailfriends.photo.ai.AiProperties;
import com.tailfriends.photo.dto.AiUsageResponse;
import com.tailfriends.photo.repository.AiUsageRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiUsageService {

    private final CurrentUser currentUser;
    private final AiUsageRepository aiUsageRepository;
    private final AiProperties aiProperties;
    private final Clock clock;

    /**
     * 오늘(KST) AI 분석 횟수. 세는 방법은 AiUsageLimiter.reserve와 같다.
     * 성공 횟수가 한도보다 적어도 실패를 포함한 시도 한도를 다 썼으면 더 분석할 수 없으므로 used를 limit으로 돌려준다.
     */
    @Transactional(readOnly = true)
    public AiUsageResponse getTodayUsage() {
        Long userId = currentUser.id();
        LocalDate today = KstDate.today(clock);
        Instant startOfToday = today.atStartOfDay(KstDate.ZONE).toInstant();
        int limit = aiProperties.dailyLimit();
        long succeeded = aiUsageRepository.countByUserIdAndSuccessTrueAndCreatedAtGreaterThanEqual(
                userId, startOfToday);
        long attempted = aiUsageRepository.countByUserIdAndCreatedAtGreaterThanEqual(userId, startOfToday);
        int used = attempted >= aiProperties.dailyAttemptLimit() ? limit : (int) Math.min(succeeded, limit);
        return new AiUsageResponse(limit, used,
                today.plusDays(1).atStartOfDay(KstDate.ZONE).toInstant());
    }
}
