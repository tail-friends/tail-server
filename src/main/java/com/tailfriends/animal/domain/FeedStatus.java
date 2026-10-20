package com.tailfriends.animal.domain;

/**
 * 급여 상태 칩. 기능명세서 4-1.
 */
public enum FeedStatus {
    /** 급여 기록 없음 (회색 "기록 없음") */
    NO_RECORD,
    /** 다음 급여일이 오늘 이후 (초록 "급여 D-n") */
    UPCOMING,
    /** 다음 급여일이 오늘 (주황 "오늘 급여") */
    DUE,
    /** 다음 급여일이 지남 (빨강 "급여 n일 지남") */
    OVERDUE,
    /** 쿨링(휴면) 기간이라 계산하지 않음 ("휴면 중") */
    COOLING
}
