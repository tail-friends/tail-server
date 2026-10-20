package com.tailfriends.log.dto;

import com.tailfriends.log.domain.LogDetails;
import com.tailfriends.log.domain.LogType;
import com.tailfriends.log.domain.ShedStage;
import com.tailfriends.log.domain.Stool;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 종류(type)에 해당하는 필드만 저장하고 나머지는 무시한다.
 * FEED: food, count, refused, dusted / SHED: stage(필수), complete / WEIGHT: value(필수, g)
 * / CLEAN: full, stool / ENV: hot, cool, humidity(하나 이상) / NOTE: memo(필수)
 */
public record LogCreateRequest(
        UUID clientId,
        @NotNull Long animalId,
        @NotNull LogType type,
        @NotNull LocalDate date,
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
