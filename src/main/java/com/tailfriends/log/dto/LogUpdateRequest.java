package com.tailfriends.log.dto;

import com.tailfriends.log.domain.LogDetails;
import com.tailfriends.log.domain.ShedStage;
import com.tailfriends.log.domain.Stool;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 보낸 필드만 바꾼다. type은 바꿀 수 없고, 기록의 종류에 해당하지 않는 필드는 무시한다.
 */
public record LogUpdateRequest(
        LocalDate date,
        @Size(max = 200) String memo,
        @Size(max = 40) String food,
        @PositiveOrZero Integer count,
        Boolean refused,
        Boolean dusted,
        ShedStage stage,
        Boolean complete,
        @Positive @Digits(integer = 6, fraction = 1) BigDecimal value,
        Boolean full,
        Stool stool,
        @Digits(integer = 3, fraction = 1) BigDecimal hot,
        @Digits(integer = 3, fraction = 1) BigDecimal cool,
        @Min(0) @Max(100) Integer humidity
) {

    public LogDetails toDetails() {
        return new LogDetails(food, count, refused, dusted, stage, complete, value, full, stool, hot, cool, humidity);
    }
}
