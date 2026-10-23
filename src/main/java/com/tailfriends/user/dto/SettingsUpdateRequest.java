package com.tailfriends.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tailfriends.user.domain.TempUnit;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalTime;

/**
 * 보낸 필드만 바꾼다. notifyTime은 "HH:mm".
 */
public record SettingsUpdateRequest(
        @JsonFormat(pattern = "HH:mm") LocalTime notifyTime,
        TempUnit tempUnit,
        Boolean keepOriginalDefault,
        @Valid AlertRulesUpdate alertRules,
        Boolean pushEnabled
) {

    public record AlertRulesUpdate(
            @Min(2) @Max(10) Integer refusalCount,
            @Min(1) @Max(50) Integer weightLossPercent,
            @Min(7) @Max(90) Integer weightLossDays,
            @Min(2) @Max(10) Integer looseStoolCount
    ) {
    }
}
