package com.tailfriends.health.dto;

import com.tailfriends.health.domain.HealthCheckItem;
import java.time.Instant;
import java.util.List;

/**
 * 건강 체크 결과 목록의 항목. 사진이 지워졌으면 photoId·thumbnailUrl은 null.
 */
public record HealthCheckSummaryResponse(
        Long checkId,
        Long photoId,
        String thumbnailUrl,
        Instant checkedAt,
        List<HealthCheckItem> items
) {
}
