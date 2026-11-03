package com.tailfriends.point.dto;

import java.util.List;

public record MyItemListResponse(
        List<MyItemResponse> items
) {
}
