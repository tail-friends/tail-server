package com.tailfriends.log.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * logIds는 요청 items 순서와 같다. undoUntil(저장 시각 + 5초)까지 되돌릴 수 있다.
 * 명세의 pointsEarned는 케어 포인트가 생기면 추가한다.
 */
public record LogBatchResponse(
        UUID batchId,
        List<Long> logIds,
        Instant undoUntil
) {
}
