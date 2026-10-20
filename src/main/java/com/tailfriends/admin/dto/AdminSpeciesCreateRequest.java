package com.tailfriends.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tailfriends.species.domain.Home;
import com.tailfriends.species.domain.Shape;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * shape, defaultHome은 대소문자를 가리지 않는다. (공개 종 목록은 소문자로 내려준다)
 * 온습도 범위는 비워 둘 수 있고, 최소가 최대보다 크면 400.
 */
public record AdminSpeciesCreateRequest(
        @NotBlank @Size(max = 40) String name,
        @NotNull @JsonFormat(with = JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_VALUES) Shape shape,
        Boolean spiky,
        @NotNull @Min(1) @Max(60) Integer feedIntervalDays,
        @Min(1) @Max(365) Integer shedIntervalDays,
        @Digits(integer = 3, fraction = 1) BigDecimal hotMin,
        @Digits(integer = 3, fraction = 1) BigDecimal hotMax,
        @Digits(integer = 3, fraction = 1) BigDecimal coolMin,
        @Digits(integer = 3, fraction = 1) BigDecimal coolMax,
        @Min(0) @Max(100) Integer humidityMin,
        @Min(0) @Max(100) Integer humidityMax,
        @NotNull @JsonFormat(with = JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_VALUES) Home defaultHome
) {
}
