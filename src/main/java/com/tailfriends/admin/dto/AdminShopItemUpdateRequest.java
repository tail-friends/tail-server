package com.tailfriends.admin.dto;

import com.tailfriends.animal.domain.RenderStyle;
import com.tailfriends.point.domain.ShopItemCategory;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 보낸 필드만 바꾼다. onSale=false면 판매 중지, true면 다시 판매.
 */
public record AdminShopItemUpdateRequest(
        @Size(max = 30) @Pattern(regexp = "(?s).*\\S.*", message = "공백만 입력할 수 없습니다") String name,
        ShopItemCategory category,
        @Min(1) @Max(100_000) Integer price,
        @Size(max = 500) @Pattern(regexp = "https://\\S+", message = "https:// 주소여야 합니다") String imageUrl,
        RenderStyle renderStyle,
        Boolean onSale
) {
}
