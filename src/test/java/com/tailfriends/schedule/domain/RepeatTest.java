package com.tailfriends.schedule.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RepeatTest {

    // 2026-10-18은 일요일
    private static final LocalDate SUNDAY = LocalDate.of(2026, 10, 18);
    private static final Repeat MON_THU = Repeat.weekdays(EnumSet.of(Weekday.THU, Weekday.MON));

    @Test
    void 기준_날짜의_요일을_확인한다() {
        assertThat(SUNDAY.getDayOfWeek()).isEqualTo(DayOfWeek.SUNDAY);
    }

    @Test
    void n일마다_반복의_첫_일정일은_시작일이다() {
        assertThat(Repeat.everyNDays(180).firstOnOrAfter(SUNDAY)).isEqualTo(SUNDAY);
    }

    @Test
    void 요일_반복의_첫_일정일은_시작일부터_첫_해당_요일이다() {
        assertThat(MON_THU.firstOnOrAfter(SUNDAY)).isEqualTo(LocalDate.of(2026, 10, 19));
        assertThat(MON_THU.firstOnOrAfter(LocalDate.of(2026, 10, 19))).isEqualTo(LocalDate.of(2026, 10, 19));
    }

    @Test
    void n일마다_반복의_다음_일정일은_완료일_더하기_n이다() {
        assertThat(Repeat.everyNDays(7).nextAfter(LocalDate.of(2026, 10, 21))).isEqualTo(LocalDate.of(2026, 10, 28));
    }

    @Test
    void 요일_반복의_다음_일정일은_완료일_다음_날부터_첫_해당_요일이다() {
        assertThat(MON_THU.nextAfter(LocalDate.of(2026, 10, 19))).isEqualTo(LocalDate.of(2026, 10, 22));
        assertThat(MON_THU.nextAfter(LocalDate.of(2026, 10, 22))).isEqualTo(LocalDate.of(2026, 10, 26));
        assertThat(MON_THU.nextAfter(LocalDate.of(2026, 10, 20))).isEqualTo(LocalDate.of(2026, 10, 22));
    }

    @Test
    void 요일은_월요일부터_정렬한다() {
        assertThat(MON_THU.days()).containsExactly(Weekday.MON, Weekday.THU);
    }

    @Test
    void n일마다_반복에_n이_없거나_범위_밖이면_INVALID_INPUT() {
        assertInvalid(() -> new Repeat(RepeatType.EVERY_N_DAYS, null, null));
        assertInvalid(() -> Repeat.everyNDays(0));
        assertInvalid(() -> Repeat.everyNDays(366));
    }

    @Test
    void 요일_반복에_요일이_없으면_INVALID_INPUT() {
        assertInvalid(() -> new Repeat(RepeatType.WEEKDAYS, null, null));
        assertInvalid(() -> Repeat.weekdays(Set.of()));
    }

    @Test
    void 반복_종류에_쓰지_않는_값은_버린다() {
        Repeat everyNDays = new Repeat(RepeatType.EVERY_N_DAYS, 3, Set.of(Weekday.MON));
        Repeat weekdays = new Repeat(RepeatType.WEEKDAYS, 3, Set.of(Weekday.MON));

        assertThat(everyNDays.days()).isEmpty();
        assertThat(weekdays.n()).isNull();
    }

    private static void assertInvalid(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }
}
