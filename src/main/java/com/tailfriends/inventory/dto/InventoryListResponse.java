package com.tailfriends.inventory.dto;

import java.util.List;

public record InventoryListResponse(List<InventoryItemResponse> items) {
}
