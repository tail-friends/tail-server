package com.tailfriends.inventory.dto;

import com.tailfriends.inventory.domain.InventoryItem;
import com.tailfriends.inventory.domain.InventoryUnit;

/**
 * low는 기준 수량이 있고 수량이 그보다 적을 때 true.
 */
public record InventoryItemResponse(
        Long id,
        String name,
        int quantity,
        InventoryUnit unit,
        Integer alertBelow,
        boolean low
) {

    public static InventoryItemResponse from(InventoryItem item) {
        return new InventoryItemResponse(item.getId(), item.getName(), item.getQuantity(), item.getUnit(),
                item.getAlertBelow(), item.isLow());
    }
}
