package com.tailfriends.enclosure.dto;

import com.tailfriends.photo.ai.EnclosureObjectType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 사육장 분석으로 읽은 장식. x, y는 화면 비율(0~1). 서버가 소수점 셋째 자리로 반올림해 저장한다.
 */
public record BackgroundObjectRequest(
        @NotNull EnclosureObjectType type,
        @NotNull @DecimalMin("0") @DecimalMax("1") BigDecimal x,
        @NotNull @DecimalMin("0") @DecimalMax("1") BigDecimal y
) {
}
