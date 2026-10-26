package com.tailfriends.schedule.dto;

import java.time.LocalDate;

public record ScheduleCreateResponse(
        Long id,
        LocalDate nextDate
) {
}
