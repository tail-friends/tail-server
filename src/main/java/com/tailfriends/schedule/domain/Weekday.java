package com.tailfriends.schedule.domain;

import java.time.DayOfWeek;

/**
 * 명세의 요일 표기(MON~SUN). 순서는 월요일부터다.
 */
public enum Weekday {
    MON,
    TUE,
    WED,
    THU,
    FRI,
    SAT,
    SUN;

    public static Weekday of(DayOfWeek dayOfWeek) {
        return values()[dayOfWeek.getValue() - 1];
    }
}
