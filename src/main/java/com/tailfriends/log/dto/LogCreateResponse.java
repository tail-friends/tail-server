package com.tailfriends.log.dto;

/**
 * 기록 작성 결과. pointsEarned, newAlerts, feed는 포인트·경고·급여일 계산이 생기면 추가한다.
 */
public record LogCreateResponse(
        Long id
) {
}
