package com.tailfriends.enclosure.domain;

import com.tailfriends.global.domain.BaseTimeEntity;
import com.tailfriends.point.domain.ShopItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사육장에 놓은 장식. 고치지 않고 배치를 저장할 때 통째로 지우고 다시 넣는다. id 순서가 그리는 순서다.
 */
@Getter
@Entity
@Table(name = "enclosure_decorations")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EnclosureDecoration extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enclosure_id", nullable = false)
    private Enclosure enclosure;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private ShopItem item;

    /**
     * 화면 비율(0~1), 소수점 셋째 자리까지.
     */
    @Column(nullable = false, precision = 4, scale = 3)
    private BigDecimal x;

    @Column(nullable = false, precision = 4, scale = 3)
    private BigDecimal y;

    @Column(nullable = false)
    private boolean flip;

    @Builder
    private EnclosureDecoration(Enclosure enclosure, ShopItem item, BigDecimal x, BigDecimal y, boolean flip) {
        this.enclosure = enclosure;
        this.item = item;
        this.x = x;
        this.y = y;
        this.flip = flip;
    }
}
