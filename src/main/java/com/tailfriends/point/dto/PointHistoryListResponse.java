package com.tailfriends.point.dto;

import java.util.List;

public record PointHistoryListResponse(
        int balance,
        List<PointHistoryResponse> history,
        Long nextCursor
) {
}
