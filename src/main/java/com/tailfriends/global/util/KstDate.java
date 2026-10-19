package com.tailfriends.global.util;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 사용자 기준 날짜(기록일, 해칭일)는 한국 시간으로 판단한다. 서버와 DB 시각은 UTC다.
 */
public final class KstDate {

    public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private KstDate() {
    }

    public static LocalDate today(Clock clock) {
        return LocalDate.now(clock.withZone(ZONE));
    }
}
