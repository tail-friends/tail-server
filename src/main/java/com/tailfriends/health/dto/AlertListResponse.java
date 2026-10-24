package com.tailfriends.health.dto;

import java.util.List;

/**
 * nextCursor는 다음 페이지가 없으면 null.
 */
public record AlertListResponse(
        List<AlertResponse> alerts,
        Long nextCursor
) {
}
