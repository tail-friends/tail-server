package com.tailfriends.schedule.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import java.time.LocalDate;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;

class ScheduleTest {

    // 2026-10-18은 일요일
    private static final LocalDate SUNDAY = LocalDate.of(2026, 10, 18);

    @Test
    void 만들면_다음_일정일은_첫_일정일이다() {
        Schedule lamp = everyNDays(180, SUNDAY, null);
        Schedule dusting = Schedule.create(null, null, "칼슘 더스팅", ScheduleKind.SUPPLEMENT,
                Repeat.weekdays(EnumSet.of(Weekday.MON, Weekday.THU)), SUNDAY, null);

        assertThat(lamp.getNextDate()).isEqualTo(SUNDAY);
        assertThat(dusting.getNextDate()).isEqualTo(LocalDate.of(2026, 10, 19));
    }

    @Test
    void 늦게_완료하면_완료한_날부터_다시_센다() {
        Schedule schedule = everyNDays(7, SUNDAY, null);

        schedule.complete(LocalDate.of(2026, 10, 21));

        assertThat(schedule.getLastDoneDate()).isEqualTo(LocalDate.of(2026, 10, 21));
        assertThat(schedule.getNextDate()).isEqualTo(LocalDate.of(2026, 10, 28));
    }

    @Test
    void 다음_일정일이_종료일을_넘으면_끝난_일정이_되고_더_완료할_수_없다() {
        Schedule schedule = everyNDays(7, SUNDAY, LocalDate.of(2026, 10, 30));

        schedule.complete(LocalDate.of(2026, 10, 25));

        assertThat(schedule.getNextDate()).isNull();
        assertInvalid(() -> schedule.complete(LocalDate.of(2026, 10, 26)));
    }

    @Test
    void 종료일이_첫_일정일보다_앞이면_만들자마자_끝난_일정이다() {
        // 일요일 시작, 월요일만 반복, 종료일도 일요일
        Schedule schedule = Schedule.create(null, null, "온욕", ScheduleKind.OTHER,
                Repeat.weekdays(EnumSet.of(Weekday.MON)), SUNDAY, SUNDAY);

        assertThat(schedule.getNextDate()).isNull();
    }

    @Test
    void 종료일이_시작일보다_앞이면_INVALID_INPUT() {
        assertInvalid(() -> everyNDays(7, SUNDAY, SUNDAY.minusDays(1)));
    }

    @Test
    void 시작일_전_날짜로_완료하면_INVALID_INPUT() {
        Schedule schedule = everyNDays(7, SUNDAY, null);

        assertInvalid(() -> schedule.complete(SUNDAY.minusDays(1)));
    }

    @Test
    void 반복을_바꾸면_마지막_완료일_기준으로_다시_계산한다() {
        Schedule schedule = everyNDays(7, SUNDAY, null);
        schedule.complete(LocalDate.of(2026, 10, 20));

        schedule.update(null, null, Repeat.everyNDays(3), null, null);

        assertThat(schedule.getNextDate()).isEqualTo(LocalDate.of(2026, 10, 23));
    }

    @Test
    void 완료한_적이_없으면_시작일_기준으로_다시_계산한다() {
        Schedule schedule = everyNDays(7, SUNDAY, null);

        schedule.update(null, null, Repeat.weekdays(EnumSet.of(Weekday.WED)), null, null);

        assertThat(schedule.getNextDate()).isEqualTo(LocalDate.of(2026, 10, 21));
    }

    @Test
    void 시작일을_마지막_완료일_뒤로_옮기면_새_시작일이_첫_일정일이다() {
        Schedule schedule = everyNDays(7, SUNDAY, null);
        schedule.complete(LocalDate.of(2026, 10, 20));

        schedule.update(null, null, null, LocalDate.of(2026, 11, 1), null);

        assertThat(schedule.getNextDate()).isEqualTo(LocalDate.of(2026, 11, 1));
    }

    @Test
    void 이름과_종류만_바꾸면_다음_일정일은_그대로다() {
        Schedule schedule = everyNDays(7, SUNDAY, null);
        schedule.complete(LocalDate.of(2026, 10, 20));

        schedule.update("UVB 램프 점검", ScheduleKind.OTHER, null, null, null);

        assertThat(schedule.getName()).isEqualTo("UVB 램프 점검");
        assertThat(schedule.getKind()).isEqualTo(ScheduleKind.OTHER);
        assertThat(schedule.getNextDate()).isEqualTo(LocalDate.of(2026, 10, 27));
    }

    @Test
    void 종료일을_늘리면_끝난_일정이_다시_열린다() {
        Schedule schedule = everyNDays(7, SUNDAY, LocalDate.of(2026, 10, 20));
        schedule.complete(LocalDate.of(2026, 10, 19));
        assertThat(schedule.getNextDate()).isNull();

        schedule.update(null, null, null, null, LocalDate.of(2026, 12, 31));

        assertThat(schedule.getNextDate()).isEqualTo(LocalDate.of(2026, 10, 26));
    }

    private static Schedule everyNDays(int n, LocalDate start, LocalDate end) {
        return Schedule.create(null, null, "UVB 램프 교체", ScheduleKind.EQUIPMENT, Repeat.everyNDays(n), start, end);
    }

    private static void assertInvalid(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }
}
