package com.tailfriends.log.dto;

import com.tailfriends.animal.dto.FeedResponse;

/**
 * 기록 작성 결과. feed는 저장 후 다시 계산한 급여 상태다. pointsEarned, newAlerts는 포인트·경고 계산이 생기면 추가한다.
 */
public record LogCreateResponse(
        Long id,
        FeedResponse feed
) {
}
