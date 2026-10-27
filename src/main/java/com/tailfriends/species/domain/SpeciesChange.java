package com.tailfriends.species.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * 종 수정 값. null인 값은 바꾸지 않는다.
 */
public record SpeciesChange(
        String name,
        Shape shape,
        Boolean spiky,
        Integer feedIntervalDays,
        Integer shedIntervalDays,
        BigDecimal hotMin,
        BigDecimal hotMax,
        BigDecimal coolMin,
        BigDecimal coolMax,
        Integer humidityMin,
        Integer humidityMax,
        Home defaultHome,
        Boolean hidden,
        Category category,
        String englishName,
        String scientificName,
        List<String> aliases,
        Integer displayOrder
) {
}
