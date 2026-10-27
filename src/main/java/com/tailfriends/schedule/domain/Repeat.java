package com.tailfriends.schedule.domain;

import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import java.time.LocalDate;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * 반복 규칙. EVERY_N_DAYS면 n만, WEEKDAYS면 days만 쓴다.
 *
 * @throws BusinessException 종류에 필요한 값이 없으면 INVALID_INPUT
 */
public record Repeat(
        RepeatType type,
        Integer n,
        Set<Weekday> days
) {

    static final int MAX_N = 365;

    public Repeat {
        if (type == null) {
            throw invalid("반복 종류가 필요해요");
        }
        if (type == RepeatType.EVERY_N_DAYS) {
            if (n == null || n < 1 || n > MAX_N) {
                throw invalid("n일마다 반복은 n이 1~365여야 해요");
            }
            days = Collections.unmodifiableSet(EnumSet.noneOf(Weekday.class));
        } else {
            if (days == null || days.isEmpty()) {
                throw invalid("요일 반복은 요일이 하나 이상 필요해요");
            }
            n = null;
            days = Collections.unmodifiableSet(EnumSet.copyOf(days));
        }
    }

    public static Repeat everyNDays(int n) {
        return new Repeat(RepeatType.EVERY_N_DAYS, n, null);
    }

    public static Repeat weekdays(Set<Weekday> days) {
        return new Repeat(RepeatType.WEEKDAYS, null, days);
    }

    /**
     * 시작일로 정한 첫 일정일. n일마다면 시작일, 요일 반복이면 시작일부터 첫 해당 요일.
     */
    public LocalDate firstOnOrAfter(LocalDate start) {
        if (type == RepeatType.EVERY_N_DAYS) {
            return start;
        }
        LocalDate date = start;
        while (!days.contains(Weekday.of(date.getDayOfWeek()))) {
            date = date.plusDays(1);
        }
        return date;
    }

    /**
     * 완료한 날 다음 일정일. n일마다면 완료일 + n, 요일 반복이면 완료일 다음 날부터 첫 해당 요일.
     */
    public LocalDate nextAfter(LocalDate done) {
        if (type == RepeatType.EVERY_N_DAYS) {
            return done.plusDays(n);
        }
        return firstOnOrAfter(done.plusDays(1));
    }

    private static BusinessException invalid(String message) {
        return new BusinessException(ErrorCode.INVALID_INPUT, message);
    }
}
