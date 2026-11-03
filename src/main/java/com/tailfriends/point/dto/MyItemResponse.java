package com.tailfriends.point.dto;

import com.tailfriends.point.domain.ShopItem;
import com.tailfriends.point.domain.ShopItemCategory;
import com.tailfriends.point.domain.UserItem;
import java.time.Instant;

/**
 * id는 상점 아이템 ID다(상점 목록의 id와 같다).
 */
public record MyItemResponse(
        Long id,
        String name,
        ShopItemCategory category,
        String imageUrl,
        Instant purchasedAt
) {

    public static MyItemResponse from(UserItem userItem) {
        ShopItem item = userItem.getItem();
        return new MyItemResponse(item.getId(), item.getName(), item.getCategory(), item.getImageUrl(),
                userItem.getCreatedAt());
    }
}
