package com.tailfriends.photo.dto;

import java.time.OffsetDateTime;

/**
 * 캐릭터·사육장·건강 체크가 같이 쓰는 오늘(KST) AI 분석 횟수.
 *
 * @param limit    하루에 할 수 있는 분석 횟수
 * @param used     오늘 쓴 횟수. 실패한 시도 한도를 다 써서 더 분석할 수 없으면 limit과 같다
 * @param resetsAt 횟수가 다시 채워지는 시각. 다음 날 0시(KST)
 */
public record AiUsageResponse(
        int limit,
        int used,
        OffsetDateTime resetsAt
) {
}
