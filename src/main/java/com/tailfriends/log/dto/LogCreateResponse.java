package com.tailfriends.log.dto;

import com.tailfriends.animal.dto.FeedResponse;
import com.tailfriends.health.dto.AlertResponse;
import java.util.List;

/**
 * 기록 작성 결과. pointsEarned는 이번 기록으로 쌓인 케어 포인트(오늘 이미 적립했거나 적립하지 않는 종류면 0),
 * feed는 저장 후 다시 계산한 급여 상태, newAlerts는 이번 기록으로 새로 생긴 건강 경고다.
 */
public record LogCreateResponse(
        Long id,
        int pointsEarned,
        List<AlertResponse> newAlerts,
        FeedResponse feed
) {
}
