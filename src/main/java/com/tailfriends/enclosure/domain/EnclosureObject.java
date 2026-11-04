package com.tailfriends.enclosure.domain;

import com.tailfriends.global.domain.BaseTimeEntity;
import com.tailfriends.photo.ai.EnclosureObjectType;
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
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사진 테마의 배경 장식. 사육장 분석으로 읽고 사용자가 확인한 것이다. 상점 아이템인 EnclosureDecoration과 다르다.
 * id 순서가 저장한 순서다.
 */
@Getter
@Entity
@Table(name = "enclosure_objects")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EnclosureObject extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enclosure_id", nullable = false)
    private Enclosure enclosure;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private EnclosureObjectType type;

    /**
     * 화면 비율(0~1), 소수점 셋째 자리까지.
     */
    @Column(nullable = false, precision = 4, scale = 3)
    private BigDecimal x;

    @Column(nullable = false, precision = 4, scale = 3)
    private BigDecimal y;

    @Builder
    private EnclosureObject(Enclosure enclosure, EnclosureObjectType type, BigDecimal x, BigDecimal y) {
        this.enclosure = enclosure;
        this.type = type;
        this.x = x;
        this.y = y;
    }
}
