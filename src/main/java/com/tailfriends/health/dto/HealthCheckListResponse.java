package com.tailfriends.health.dto;

import java.util.List;

public record HealthCheckListResponse(
        List<HealthCheckSummaryResponse> checks,
        Long nextCursor
) {
}
