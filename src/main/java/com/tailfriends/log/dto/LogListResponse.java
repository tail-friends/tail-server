package com.tailfriends.log.dto;

import java.util.List;

/**
 * nextCursor는 다음 페이지가 없으면 null.
 */
public record LogListResponse(
        List<LogResponse> logs,
        Long nextCursor
) {
}
