package com.tailfriends.species.dto;

import com.tailfriends.species.domain.Species;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

public record SpeciesResponse(
        Long id,
        String name,
        String shape,
        boolean spiky,
        int feedIntervalDays,
        Integer shedIntervalDays,
        EnvRange envRange,
        String defaultHome,
        String category,
        String englishName,
        String scientificName,
        List<String> aliases
) {

    public static SpeciesResponse from(Species species) {
        return new SpeciesResponse(
                species.getId(),
                species.getName(),
                species.getShape().name().toLowerCase(Locale.ROOT),
                species.isSpiky(),
                species.getFeedIntervalDays(),
                species.getShedIntervalDays(),
                new EnvRange(
                        Range.of(species.getHotMin(), species.getHotMax()),
                        Range.of(species.getCoolMin(), species.getCoolMax()),
                        Range.of(species.getHumidityMin(), species.getHumidityMax())),
                species.getDefaultHome().name().toLowerCase(Locale.ROOT),
                species.getCategory().name().toLowerCase(Locale.ROOT),
                species.getEnglishName(),
                species.getScientificName(),
                List.copyOf(species.getAliases()));
    }

    public record EnvRange(
            Range<BigDecimal> hot,
            Range<BigDecimal> cool,
            Range<Integer> humidity
    ) {
    }

    public record Range<T extends Number>(
            T min,
            T max
    ) {

        static <T extends Number> Range<T> of(T min, T max) {
            if (min == null && max == null) {
                return null;
            }
            return new Range<>(min, max);
        }
    }
}
