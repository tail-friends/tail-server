package com.tailfriends.inventory.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * add(현재 수량에 더하기)와 quantity(그 값으로 바꾸기) 중 하나만 보낸다. 서비스가 확인한다.
 */
public record InventoryUpdateRequest(
        @Min(1) @Max(InventoryLimits.MAX_QUANTITY) Integer add,
        @Min(0) @Max(InventoryLimits.MAX_QUANTITY) Integer quantity
) {
}
