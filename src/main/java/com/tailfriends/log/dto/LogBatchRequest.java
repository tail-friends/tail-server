package com.tailfriends.log.dto;

import com.tailfriends.log.domain.LogDetails;
import com.tailfriends.log.domain.LogType;
import com.tailfriends.log.domain.Stool;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * 여러 개체의 같은 종류 기록. type은 FEED, CLEAN, WEIGHT만 되고 date는 전체 공통이다.
 * 항목 필드는 기록 작성과 같다. FEED: food, count, refused, dusted / CLEAN: full, stool / WEIGHT: value(필수)
 */
public record LogBatchRequest(
        @NotNull LogType type,
        @NotNull LocalDate date,
        @NotEmpty @Size(max = 30) List<@NotNull @Valid Item> items
) {

    public record Item(
            UUID clientId,
            @NotNull Long animalId,
            @Size(max = 200) String memo,
            @Size(max = 40) String food,
            @PositiveOrZero Integer count,
            Boolean refused,
            Boolean dusted,
            @Positive @Digits(integer = 6, fraction = 1) BigDecimal value,
            Boolean full,
            Stool stool
    ) {

        public LogDetails toDetails() {
            return new LogDetails(food, count, refused, dusted, null, null, value, full, stool, null, null, null);
        }
    }
}
