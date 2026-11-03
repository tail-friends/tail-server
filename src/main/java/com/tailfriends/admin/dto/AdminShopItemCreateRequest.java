package com.tailfriends.admin.dto;

import com.tailfriends.animal.domain.RenderStyle;
import com.tailfriends.point.domain.ShopItemCategory;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * name은 앞뒤 공백을 빼고 저장한다. renderStyle을 생략하면 ILLUSTRATION_2D, onSale을 생략하면 true.
 */
public record AdminShopItemCreateRequest(
        @NotBlank @Size(max = 30) String name,
        @NotNull ShopItemCategory category,
        @NotNull @Min(1) @Max(100_000) Integer price,
        @NotNull @Size(max = 500) @Pattern(regexp = "https://\\S+", message = "https:// 주소여야 합니다")
        String imageUrl,
        RenderStyle renderStyle,
        Boolean onSale
) {
}
