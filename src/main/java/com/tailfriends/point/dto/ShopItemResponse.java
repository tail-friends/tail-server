package com.tailfriends.point.dto;

import com.tailfriends.animal.domain.RenderStyle;
import com.tailfriends.point.domain.ShopItem;
import com.tailfriends.point.domain.ShopItemCategory;

/**
 * owned는 내가 이미 산 아이템이면 true.
 */
public record ShopItemResponse(
        Long id,
        String name,
        ShopItemCategory category,
        int price,
        String imageUrl,
        RenderStyle renderStyle,
        boolean owned
) {

    public static ShopItemResponse of(ShopItem item, boolean owned) {
        return new ShopItemResponse(item.getId(), item.getName(), item.getCategory(), item.getPrice(),
                item.getImageUrl(), item.getRenderStyle(), owned);
    }
}
