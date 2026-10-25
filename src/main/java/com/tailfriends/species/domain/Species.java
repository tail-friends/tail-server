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
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "species")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Species extends BaseTimeEntity {

    static final int DEFAULT_DISPLAY_ORDER = 1000;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Category category;

    @Column(length = 60)
    private String englishName;

    @Column(length = 80)
    private String scientificName;

    /** 다른 한글 이름·표기. 앱 검색에 쓴다. */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false, columnDefinition = "varchar(40)[]")
    private List<String> aliases = new ArrayList<>();

    /** 작을수록 종 목록 앞에 나온다. 같으면 이름순. */
    @Column(nullable = false)
    private int displayOrder;

    /**
     * category를 비우면 REPTILE, displayOrder를 비우면 1000(맨 뒤쪽)이다.
     */
    @Builder
    private Species(String name, Shape shape, boolean spiky, int feedIntervalDays, Integer shedIntervalDays,
                    BigDecimal hotMin, BigDecimal hotMax, BigDecimal coolMin, BigDecimal coolMax,
                    Integer humidityMin, Integer humidityMax, Home defaultHome, boolean hidden,
                    Category category, String englishName, String scientificName, List<String> aliases,
                    Integer displayOrder) {
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
        this.category = category != null ? category : Category.REPTILE;
        this.englishName = englishName;
        this.scientificName = scientificName;
        this.aliases = normalize(aliases);
        this.displayOrder = displayOrder != null ? displayOrder : DEFAULT_DISPLAY_ORDER;
        validateRanges();
    }

    /**
     * null인 값은 바꾸지 않는다. aliases는 보낸 목록으로 통째로 바꾼다(빈 목록이면 모두 지움).
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
        category = valueOr(change.category(), category);
        englishName = valueOr(change.englishName(), englishName);
        scientificName = valueOr(change.scientificName(), scientificName);
        if (change.aliases() != null) {
            aliases = normalize(change.aliases());
        }
        displayOrder = valueOr(change.displayOrder(), displayOrder);
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

    /**
     * 앞뒤 공백을 빼고, 빈 값과 중복은 뺀다. 순서는 유지한다.
     */
    private static List<String> normalize(List<String> aliases) {
        if (aliases == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(aliases.stream()
                .filter(alias -> alias != null && !alias.isBlank())
                .map(String::strip)
                .distinct()
                .toList());
    }

    private static <T> T valueOr(T value, T fallback) {
        return value != null ? value : fallback;
    }
}
