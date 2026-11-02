package com.tailfriends.inventory.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class InventoryItemTest {

    @Test
    void 수량이_기준_수량보다_적으면_부족이다() {
        assertThat(item(29, 30).isLow()).isTrue();
    }

    @Test
    void 수량이_기준_수량과_같거나_많으면_부족이_아니다() {
        assertThat(item(30, 30).isLow()).isFalse();
        assertThat(item(100, 30).isLow()).isFalse();
    }

    @Test
    void 기준_수량이_없으면_0개여도_부족이_아니다() {
        assertThat(item(0, null).isLow()).isFalse();
    }

    private static InventoryItem item(int quantity, Integer alertBelow) {
        return InventoryItem.builder().name("귀뚜라미").quantity(quantity).unit(InventoryUnit.COUNT)
                .alertBelow(alertBelow).build();
    }
}
