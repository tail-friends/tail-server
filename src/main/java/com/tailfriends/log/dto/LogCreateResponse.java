package com.tailfriends.log.dto;

import com.tailfriends.animal.dto.FeedResponse;
import com.tailfriends.health.dto.AlertResponse;
import java.util.List;

/**
 * 기록 작성 결과. feed는 저장 후 다시 계산한 급여 상태, newAlerts는 이번 기록으로 새로 생긴 건강 경고다.
 * pointsEarned는 케어 포인트가 생기면 추가한다.
 */
public record LogCreateResponse(
        Long id,
        List<AlertResponse> newAlerts,
        FeedResponse feed
) {
}
