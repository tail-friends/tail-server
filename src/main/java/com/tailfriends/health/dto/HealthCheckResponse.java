package com.tailfriends.health.dto;

import com.tailfriends.health.domain.HealthCheck;
import com.tailfriends.health.domain.HealthCheckItem;
import java.time.Instant;
import java.util.List;

/**
 * 사진 건강 체크 결과. disclaimer는 앱이 결과 아래에 그대로 보여 주는 문구다.
 */
public record HealthCheckResponse(
        Long checkId,
        Instant checkedAt,
        List<HealthCheckItem> items,
        String disclaimer
) {

    public static final String DISCLAIMER = "이 결과는 참고용이에요. 이상이 의심되면 특수동물 병원에 문의하세요.";

    public static HealthCheckResponse from(HealthCheck check) {
        return new HealthCheckResponse(check.getId(), check.getCreatedAt(), check.getItems(), DISCLAIMER);
    }
}
