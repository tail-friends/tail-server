package com.tailfriends.point.dto;

import com.tailfriends.point.domain.PointHistory;
import com.tailfriends.point.domain.PointReason;
import java.time.Instant;

/**
 * animalId·logId는 기록 적립일 때만 있다. 개체나 기록을 지웠으면 null이다.
 */
public record PointHistoryResponse(
        Long id,
        int amount,
        PointReason reason,
        Long animalId,
        Long logId,
        Instant createdAt
) {

    public static PointHistoryResponse from(PointHistory history) {
        return new PointHistoryResponse(
                history.getId(),
                history.getAmount(),
                history.getReason(),
                history.getAnimal() == null ? null : history.getAnimal().getId(),
                history.getLog() == null ? null : history.getLog().getId(),
                history.getCreatedAt());
    }
}
