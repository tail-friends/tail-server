package com.tailfriends.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tailfriends.species.domain.Category;
import com.tailfriends.species.domain.Home;
import com.tailfriends.species.domain.Shape;
import com.tailfriends.species.domain.SpeciesChange;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * 보낸 필드만 바꾼다. hidden=false로 숨긴 종을 다시 보이게 할 수 있다.
 * aliases는 보낸 목록으로 통째로 바꾼다(빈 목록이면 모두 지움).
 */
public record AdminSpeciesUpdateRequest(
        @Size(max = 40) @Pattern(regexp = "(?s).*\\S.*", message = "공백만 입력할 수 없습니다") String name,
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_VALUES) Shape shape,
        Boolean spiky,
        @Min(1) @Max(60) Integer feedIntervalDays,
        @Min(1) @Max(365) Integer shedIntervalDays,
        @Digits(integer = 3, fraction = 1) BigDecimal hotMin,
        @Digits(integer = 3, fraction = 1) BigDecimal hotMax,
        @Digits(integer = 3, fraction = 1) BigDecimal coolMin,
        @Digits(integer = 3, fraction = 1) BigDecimal coolMax,
        @Min(0) @Max(100) Integer humidityMin,
        @Min(0) @Max(100) Integer humidityMax,
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_VALUES) Home defaultHome,
        Boolean hidden,
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_VALUES) Category category,
        @Size(max = 60) String englishName,
        @Size(max = 80) String scientificName,
        @Size(max = 10) List<@NotBlank @Size(max = 40) String> aliases,
        @Min(0) @Max(10000) Integer displayOrder
) {

    public SpeciesChange toChange() {
        return new SpeciesChange(name == null ? null : name.strip(), shape, spiky, feedIntervalDays, shedIntervalDays,
                hotMin, hotMax, coolMin, coolMax, humidityMin, humidityMax, defaultHome, hidden,
                category, stripOrNull(englishName), stripOrNull(scientificName), aliases, displayOrder);
    }

    private static String stripOrNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
