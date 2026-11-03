package com.tailfriends.enclosure.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * x, y는 화면 비율(0~1). 서버가 소수점 셋째 자리로 반올림해 저장한다. flip을 생략하면 false.
 */
public record DecorationRequest(
        @NotNull Long itemId,
        @NotNull @DecimalMin("0") @DecimalMax("1") BigDecimal x,
        @NotNull @DecimalMin("0") @DecimalMax("1") BigDecimal y,
        Boolean flip
) {
}
