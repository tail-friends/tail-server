package com.tailfriends.today.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tailfriends.animal.domain.FeedStatus;
import com.tailfriends.health.domain.AlertType;
import java.time.LocalDate;
import java.util.List;

/**
 * 오늘 할 일. feeds는 급여 DUE·OVERDUE 개체, schedules는 오늘이거나 지난 일정, alerts는 열린 경고,
 * done은 오늘 남긴 기록과 오늘 완료한 일정.
 */
public record TodayResponse(
        LocalDate date,
        List<TodayFeed> feeds,
        List<TodaySchedule> schedules,
        List<TodayAlert> alerts,
        List<TodayDone> done
) {

    /**
     * overdueDays는 OVERDUE일 때만. thumbnailUrl·suggested는 없으면 null.
     */
    public record TodayFeed(
            Long animalId,
            String name,
            String thumbnailUrl,
            FeedStatus status,
            @JsonInclude(JsonInclude.Include.NON_NULL) Integer overdueDays,
            Suggested suggested
    ) {
    }

    /**
     * 가장 최근에 먹은 급여 기록의 먹이와 개수.
     */
    public record Suggested(
            String food,
            Integer count
    ) {
    }

    /**
     * animalId가 null이면 공통 일정. nextDate가 오늘보다 앞이면 지난 일정.
     */
    public record TodaySchedule(
            Long scheduleId,
            Long animalId,
            String name,
            LocalDate nextDate
    ) {
    }

    public record TodayAlert(
            Long id,
            Long animalId,
            AlertType type
    ) {
    }

    /**
     * 기록이면 type이 기록 종류이고 logId가, 일정이면 type이 SCHEDULE이고 scheduleId가 있다.
     */
    public record TodayDone(
            String type,
            Long animalId,
            @JsonInclude(JsonInclude.Include.NON_NULL) Long logId,
            @JsonInclude(JsonInclude.Include.NON_NULL) Long scheduleId
    ) {

        public static final String SCHEDULE = "SCHEDULE";
    }
}
