package com.tailfriends.photo.ai;

import com.tailfriends.health.domain.HealthCheckItem;
import java.util.List;

/**
 * AI가 사진에서 읽은 건강 체크 결과. reptile이 false면 items는 비어 있다.
 */
public record HealthCheckAnalysis(
        boolean reptile,
        List<HealthCheckItem> items
) {

    public static HealthCheckAnalysis notReptile() {
        return new HealthCheckAnalysis(false, List.of());
    }
}
