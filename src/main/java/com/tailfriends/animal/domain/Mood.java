package com.tailfriends.animal.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 펫 말풍선 고르기용 상태. 문구는 앱이 가진다. 기능명세서 4-6, 펫 말풍선(F-14).
 * 여러 조건이 겹치면 위에 있는 것이 먼저다.
 */
public enum Mood {
    /** 쿨링(휴면) 중. 말하지 않고 잠든 모습 */
    SLEEPING,
    /** 열린 건강 경고가 있음 ("기운이 없어요") */
    UNWELL,
    /** 급여일이 지남 ("배고파요…") */
    HUNGRY,
    /** 오늘이 급여일 ("오늘 밥 먹는 날!") */
    FEED_DAY,
    /** 탈피한 지 2일 이내 ("허물 벗고 개운해요") */
    JUST_SHED,
    /** 해당 없음 (평범한 한마디) */
    NORMAL;

    static final int JUST_SHED_DAYS = 2;

    public static Mood of(FeedSchedule feed, boolean hasOpenAlert, LocalDate lastShedDate, LocalDate today) {
        if (feed.status() == FeedStatus.COOLING) {
            return SLEEPING;
        }
        if (hasOpenAlert) {
            return UNWELL;
        }
        if (feed.status() == FeedStatus.OVERDUE) {
            return HUNGRY;
        }
        if (feed.status() == FeedStatus.DUE) {
            return FEED_DAY;
        }
        if (lastShedDate != null) {
            long days = ChronoUnit.DAYS.between(lastShedDate, today);
            if (days >= 0 && days <= JUST_SHED_DAYS) {
                return JUST_SHED;
            }
        }
        return NORMAL;
    }
}
