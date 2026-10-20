package com.tailfriends.animal.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class FeedScheduleTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 20);

    @Test
    void 급여_기록이_없으면_NO_RECORD() {
        FeedSchedule schedule = FeedSchedule.calculate(null, 4, null, null, TODAY);

        assertThat(schedule).isEqualTo(new FeedSchedule(FeedStatus.NO_RECORD, null, null, null));
    }

    @Test
    void 다음_급여일이_오늘_이후면_UPCOMING과_남은_일수() {
        FeedSchedule schedule = FeedSchedule.calculate(LocalDate.of(2026, 10, 18), 4, null, null, TODAY);

        assertThat(schedule.status()).isEqualTo(FeedStatus.UPCOMING);
        assertThat(schedule.lastDate()).isEqualTo(LocalDate.of(2026, 10, 18));
        assertThat(schedule.nextDate()).isEqualTo(LocalDate.of(2026, 10, 22));
        assertThat(schedule.daysLeft()).isEqualTo(2);
    }

    @Test
    void 다음_급여일이_오늘이면_DUE() {
        FeedSchedule schedule = FeedSchedule.calculate(LocalDate.of(2026, 10, 16), 4, null, null, TODAY);

        assertThat(schedule.status()).isEqualTo(FeedStatus.DUE);
        assertThat(schedule.daysLeft()).isZero();
    }

    @Test
    void 다음_급여일이_지났으면_OVERDUE와_음수_일수() {
        FeedSchedule schedule = FeedSchedule.calculate(LocalDate.of(2026, 10, 13), 4, null, null, TODAY);

        assertThat(schedule.status()).isEqualTo(FeedStatus.OVERDUE);
        assertThat(schedule.nextDate()).isEqualTo(LocalDate.of(2026, 10, 17));
        assertThat(schedule.daysLeft()).isEqualTo(-3);
    }

    @Test
    void 월을_넘어가도_날짜로_계산한다() {
        FeedSchedule schedule = FeedSchedule.calculate(LocalDate.of(2026, 9, 28), 10, null, null, TODAY);

        assertThat(schedule.nextDate()).isEqualTo(LocalDate.of(2026, 10, 8));
        assertThat(schedule.daysLeft()).isEqualTo(-12);
    }

    @Test
    void 쿨링_기간이면_계산하지_않고_COOLING() {
        FeedSchedule schedule = FeedSchedule.calculate(LocalDate.of(2026, 9, 1), 4,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 31), TODAY);

        assertThat(schedule).isEqualTo(new FeedSchedule(FeedStatus.COOLING, LocalDate.of(2026, 9, 1), null, null));
    }

    @Test
    void 쿨링_종료일이_없으면_시작일부터_계속_쿨링이다() {
        FeedSchedule schedule = FeedSchedule.calculate(null, 4, TODAY, null, TODAY);

        assertThat(schedule.status()).isEqualTo(FeedStatus.COOLING);
    }

    @Test
    void 쿨링이_끝났으면_평소대로_계산한다() {
        FeedSchedule schedule = FeedSchedule.calculate(LocalDate.of(2026, 10, 18), 4,
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 19), TODAY);

        assertThat(schedule.status()).isEqualTo(FeedStatus.UPCOMING);
    }
}
