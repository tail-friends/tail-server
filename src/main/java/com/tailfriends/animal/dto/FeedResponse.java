package com.tailfriends.animal.dto;

import com.tailfriends.animal.domain.FeedSchedule;
import com.tailfriends.animal.domain.FeedStatus;
import java.time.LocalDate;

/**
 * 급여 상태 칩. 기록이 없거나 쿨링 중이면 nextDate와 daysLeft는 null. daysLeft가 음수면 그만큼 지났다는 뜻이다.
 */
public record FeedResponse(
        FeedStatus status,
        LocalDate lastDate,
        LocalDate nextDate,
        Integer daysLeft
) {

    public static FeedResponse from(FeedSchedule schedule) {
        return new FeedResponse(schedule.status(), schedule.lastDate(), schedule.nextDate(), schedule.daysLeft());
    }
}
