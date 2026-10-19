package com.tailfriends.species.domain;

import com.tailfriends.global.domain.BaseTimeEntity;
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
    }
}
