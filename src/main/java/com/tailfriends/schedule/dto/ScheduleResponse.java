package com.tailfriends.schedule.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tailfriends.schedule.domain.Repeat;
import com.tailfriends.schedule.domain.RepeatType;
import com.tailfriends.schedule.domain.Schedule;
import com.tailfriends.schedule.domain.ScheduleKind;
import com.tailfriends.schedule.domain.Weekday;
import java.time.LocalDate;
import java.util.List;

/**
 * animalId가 null이면 공통 일정, nextDate가 null이면 끝난 일정.
 */
public record ScheduleResponse(
        Long id,
        Long animalId,
        String name,
        ScheduleKind kind,
        RepeatResponse repeat,
        LocalDate startDate,
        LocalDate nextDate,
        LocalDate lastDoneDate,
        LocalDate endDate
) {

    public static ScheduleResponse from(Schedule schedule) {
        return new ScheduleResponse(
                schedule.getId(),
                schedule.getAnimal() == null ? null : schedule.getAnimal().getId(),
                schedule.getName(),
                schedule.getKind(),
                RepeatResponse.from(schedule.repeat()),
                schedule.getStartDate(),
                schedule.getNextDate(),
                schedule.getLastDoneDate(),
                schedule.getEndDate());
    }

    /**
     * EVERY_N_DAYS면 n만, WEEKDAYS면 days(월요일부터)만 내려준다.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record RepeatResponse(
            RepeatType type,
            Integer n,
            List<Weekday> days
    ) {

        static RepeatResponse from(Repeat repeat) {
            List<Weekday> days = repeat.type() == RepeatType.WEEKDAYS ? List.copyOf(repeat.days()) : null;
            return new RepeatResponse(repeat.type(), repeat.n(), days);
        }
    }
}
