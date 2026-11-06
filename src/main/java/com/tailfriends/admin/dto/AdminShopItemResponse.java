package com.tailfriends.admin.dto;

import com.tailfriends.animal.domain.RenderStyle;
import com.tailfriends.point.domain.ShopItem;
import com.tailfriends.point.domain.ShopItemCategory;

public record AdminShopItemResponse(
        Long id,
        String name,
        ShopItemCategory category,
        int price,
        String imageUrl,
        RenderStyle renderStyle,
        boolean onSale
) {

    public static AdminShopItemResponse from(ShopItem item) {
        return new AdminShopItemResponse(item.getId(), item.getName(), item.getCategory(), item.getPrice(),
                item.getImageUrl(), item.getRenderStyle(), item.isOnSale());
    }
}
