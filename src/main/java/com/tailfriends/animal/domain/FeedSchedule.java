package com.tailfriends.animal.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 다음 급여일 계산 결과. 기능명세서 4-1.
 *
 * @param lastDate 마지막 급여일 ("안 먹음" 기록 제외). 기록이 없으면 null
 * @param nextDate 다음 급여일 = lastDate + 급여 주기. 기록이 없거나 쿨링 중이면 null
 * @param daysLeft 오늘부터 nextDate까지 남은 일수. 지났으면 음수 (-2 = 2일 지남)
 */
public record FeedSchedule(
        FeedStatus status,
        LocalDate lastDate,
        LocalDate nextDate,
        Integer daysLeft
) {

    public static FeedSchedule of(Animal animal, LocalDate lastFedDate, LocalDate today) {
        return calculate(lastFedDate, animal.getFeedIntervalDays(),
                animal.getCoolingStart(), animal.getCoolingEnd(), today);
    }

    /**
     * 쿨링 기간(coolingEnd가 없으면 시작일부터 계속)에는 COOLING. 오늘은 한국 날짜 기준으로 넘긴다.
     */
    public static FeedSchedule calculate(LocalDate lastFedDate, int intervalDays,
                                         LocalDate coolingStart, LocalDate coolingEnd, LocalDate today) {
        if (isCooling(coolingStart, coolingEnd, today)) {
            return new FeedSchedule(FeedStatus.COOLING, lastFedDate, null, null);
        }
        if (lastFedDate == null) {
            return new FeedSchedule(FeedStatus.NO_RECORD, null, null, null);
        }
        LocalDate nextDate = lastFedDate.plusDays(intervalDays);
        int daysLeft = (int) ChronoUnit.DAYS.between(today, nextDate);
        FeedStatus status = daysLeft > 0 ? FeedStatus.UPCOMING
                : daysLeft == 0 ? FeedStatus.DUE
                : FeedStatus.OVERDUE;
        return new FeedSchedule(status, lastFedDate, nextDate, daysLeft);
    }

    private static boolean isCooling(LocalDate coolingStart, LocalDate coolingEnd, LocalDate today) {
        return coolingStart != null
                && !today.isBefore(coolingStart)
                && (coolingEnd == null || !today.isAfter(coolingEnd));
    }
}
