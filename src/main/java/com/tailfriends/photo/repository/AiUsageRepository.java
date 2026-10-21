package com.tailfriends.photo.repository;

import com.tailfriends.photo.domain.AiUsage;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiUsageRepository extends JpaRepository<AiUsage, Long> {

    /**
     * 하루 횟수 제한은 AI가 답을 준 분석만 센다. AI 쪽 오류로 실패한 분석은 세지 않는다.
     */
    long countByUserIdAndSuccessTrueAndCreatedAtGreaterThanEqual(Long userId, Instant from);
}
