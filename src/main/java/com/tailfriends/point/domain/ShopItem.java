package com.tailfriends.point.domain;

import com.tailfriends.animal.domain.RenderStyle;
import com.tailfriends.global.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 케어 포인트 상점 아이템. 관리자가 넣는다. 산 사람의 보유 목록에 남아야 해서 지우지 않고 판매를 중지한다.
 */
@Getter
@Entity
@Table(name = "shop_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShopItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private ShopItemCategory category;

    @Column(nullable = false)
    private int price;

    @Column(nullable = false, length = 500)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RenderStyle renderStyle;

    @Column(nullable = false)
    private boolean onSale;

    @Builder
    private ShopItem(String name, ShopItemCategory category, int price, String imageUrl, RenderStyle renderStyle,
                     boolean onSale) {
        this.name = name;
        this.category = category;
        this.price = price;
        this.imageUrl = imageUrl;
        this.renderStyle = renderStyle;
        this.onSale = onSale;
    }

    /**
     * null인 값은 바꾸지 않는다.
     */
    public void update(String name, ShopItemCategory category, Integer price, String imageUrl,
                       RenderStyle renderStyle, Boolean onSale) {
        if (name != null) {
            this.name = name;
        }
        if (category != null) {
            this.category = category;
        }
        if (price != null) {
            this.price = price;
        }
        if (imageUrl != null) {
            this.imageUrl = imageUrl;
        }
        if (renderStyle != null) {
            this.renderStyle = renderStyle;
        }
        if (onSale != null) {
            this.onSale = onSale;
        }
    }
}
