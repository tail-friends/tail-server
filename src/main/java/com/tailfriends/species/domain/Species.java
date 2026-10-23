package com.tailfriends.species.domain;

import com.tailfriends.global.domain.BaseTimeEntity;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "species")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Species extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Shape shape;

    @Column(nullable = false)
    private boolean spiky;

    @Column(nullable = false)
    private int feedIntervalDays;

    private Integer shedIntervalDays;

    @Column(precision = 4, scale = 1)
    private BigDecimal hotMin;

    @Column(precision = 4, scale = 1)
    private BigDecimal hotMax;

    @Column(precision = 4, scale = 1)
    private BigDecimal coolMin;

    @Column(precision = 4, scale = 1)
    private BigDecimal coolMax;

    private Integer humidityMin;

    private Integer humidityMax;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Home defaultHome;

    @Column(nullable = false)
    private boolean hidden;

    @Builder
    private Species(String name, Shape shape, boolean spiky, int feedIntervalDays, Integer shedIntervalDays,
                    BigDecimal hotMin, BigDecimal hotMax, BigDecimal coolMin, BigDecimal coolMax,
                    Integer humidityMin, Integer humidityMax, Home defaultHome, boolean hidden) {
        this.name = name;
        this.shape = shape;
        this.spiky = spiky;
        this.feedIntervalDays = feedIntervalDays;
        this.shedIntervalDays = shedIntervalDays;
        this.hotMin = hotMin;
        this.hotMax = hotMax;
        this.coolMin = coolMin;
        this.coolMax = coolMax;
        this.humidityMin = humidityMin;
        this.humidityMax = humidityMax;
        this.defaultHome = defaultHome;
        this.hidden = hidden;
        validateRanges();
    }

    /**
     * null인 값은 바꾸지 않는다.
     *
     * @throws BusinessException 바꾼 뒤 온습도 범위의 최소가 최대보다 크면 INVALID_INPUT
     */
    public void update(SpeciesChange change) {
        name = valueOr(change.name(), name);
        shape = valueOr(change.shape(), shape);
        spiky = valueOr(change.spiky(), spiky);
        feedIntervalDays = valueOr(change.feedIntervalDays(), feedIntervalDays);
        shedIntervalDays = valueOr(change.shedIntervalDays(), shedIntervalDays);
        hotMin = valueOr(change.hotMin(), hotMin);
        hotMax = valueOr(change.hotMax(), hotMax);
        coolMin = valueOr(change.coolMin(), coolMin);
        coolMax = valueOr(change.coolMax(), coolMax);
        humidityMin = valueOr(change.humidityMin(), humidityMin);
        humidityMax = valueOr(change.humidityMax(), humidityMax);
        defaultHome = valueOr(change.defaultHome(), defaultHome);
        hidden = valueOr(change.hidden(), hidden);
        validateRanges();
    }

    /**
     * 새 개체 등록의 종 목록에서만 빠진다. 이미 이 종으로 등록된 개체는 그대로다.
     */
    public void hide() {
        hidden = true;
    }

    private void validateRanges() {
        requireOrdered(hotMin, hotMax, "핫존 온도");
        requireOrdered(coolMin, coolMax, "쿨존 온도");
        requireOrdered(humidityMin, humidityMax, "습도");
    }

    private static <T extends Comparable<T>> void requireOrdered(T min, T max, String label) {
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, label + "의 최소값이 최대값보다 커요");
        }
    }

    private static <T> T valueOr(T value, T fallback) {
        return value != null ? value : fallback;
    }
}
