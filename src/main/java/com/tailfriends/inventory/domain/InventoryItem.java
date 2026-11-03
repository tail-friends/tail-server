package com.tailfriends.inventory.domain;

import com.tailfriends.global.domain.BaseTimeEntity;
import com.tailfriends.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 먹이 재고 품목. 사용자마다 이름이 겹치지 않는다. 수량 범위는 서비스가 확인한다.
 */
@Getter
@Entity
@Table(name = "inventory_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 40)
    private String name;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private InventoryUnit unit;

    private Integer alertBelow;

    @Builder
    private InventoryItem(User user, String name, int quantity, InventoryUnit unit, Integer alertBelow) {
        this.user = user;
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.alertBelow = alertBelow;
    }

    public void changeQuantity(int quantity) {
        this.quantity = quantity;
    }

    /**
     * 기준 수량이 있고 그보다 적으면 true.
     */
    public boolean isLow() {
        return alertBelow != null && quantity < alertBelow;
    }
}
