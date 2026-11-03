package com.tailfriends.point.dto;

import java.util.List;

public record ShopItemListResponse(
        List<ShopItemResponse> items
) {
}
