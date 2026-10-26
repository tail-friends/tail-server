package com.tailfriends.schedule.dto;

import com.tailfriends.schedule.domain.Repeat;
import com.tailfriends.schedule.domain.RepeatType;
import com.tailfriends.schedule.domain.Weekday;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

/**
 * EVERY_N_DAYS면 n(1~365), WEEKDAYS면 days(하나 이상)가 필요하다. 다른 쪽 값은 무시한다.
 */
public record RepeatRequest(
        @NotNull RepeatType type,
        @Min(1) @Max(365) Integer n,
        Set<Weekday> days
) {

    public Repeat toRepeat() {
        return new Repeat(type, n, days);
    }
}
