package com.tailfriends.schedule.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * date는 실제로 한 날. 오늘(KST) 이후는 안 된다.
 */
public record ScheduleCompleteRequest(
        @NotNull LocalDate date
) {
}
