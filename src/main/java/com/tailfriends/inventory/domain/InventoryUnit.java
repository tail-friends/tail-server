package com.tailfriends.inventory.domain;

/**
 * 급여 기록의 count는 개수라서 자동 차감은 COUNT 품목만 한다.
 */
public enum InventoryUnit {
    COUNT,
    GRAM
}
