package com.tailfriends.breeding.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 산란일은 메이팅일부터 오늘(KST)까지, 예상 해칭일은 산란일과 같거나 늦게(서비스가 확인). 온도는 ℃.
 */
public record ClutchRequest(
        @NotNull LocalDate laidDate,
        @NotNull @Min(1) @Max(99) Integer eggCount,
        @DecimalMin("10.0") @DecimalMax("40.0") @Digits(integer = 2, fraction = 1) BigDecimal incubationTemp,
        LocalDate expectedHatchDate
) {
}
