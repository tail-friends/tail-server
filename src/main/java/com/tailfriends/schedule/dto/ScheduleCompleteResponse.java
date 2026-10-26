package com.tailfriends.schedule.dto;

import java.time.LocalDate;

/**
 * nextDate가 null이면 이번 완료로 일정이 끝났다(endDate를 넘음).
 */
public record ScheduleCompleteResponse(
        LocalDate lastDoneDate,
        LocalDate nextDate
) {
}
