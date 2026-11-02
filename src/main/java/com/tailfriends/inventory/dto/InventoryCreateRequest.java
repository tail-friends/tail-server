package com.tailfriends.inventory.dto;

import com.tailfriends.inventory.domain.InventoryUnit;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * name은 앞뒤 공백을 빼고 저장한다. 급여 기록의 food(최대 40자)와 같으면 자동 차감한다.
 */
public record InventoryCreateRequest(
        @NotBlank @Size(max = 40) String name,
        @NotNull @Min(0) @Max(InventoryLimits.MAX_QUANTITY) Integer quantity,
        @NotNull InventoryUnit unit,
        @Min(0) @Max(InventoryLimits.MAX_QUANTITY) Integer alertBelow
) {
}
