package com.tailfriends.animal.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class MoodTest {

    private static final LocalDate TODAY = LocalDate.parse("2026-10-21");

    @Test
    void 쿨링_중이면_다른_조건보다_먼저_SLEEPING() {
        assertThat(Mood.of(feed(FeedStatus.COOLING), true, TODAY, TODAY)).isEqualTo(Mood.SLEEPING);
    }

    @Test
    void 열린_건강_경고가_있으면_급여일이_지나도_UNWELL() {
        assertThat(Mood.of(feed(FeedStatus.OVERDUE), true, null, TODAY)).isEqualTo(Mood.UNWELL);
    }

    @Test
    void 급여일이_지나면_HUNGRY() {
        assertThat(Mood.of(feed(FeedStatus.OVERDUE), false, TODAY, TODAY)).isEqualTo(Mood.HUNGRY);
    }

    @Test
    void 오늘이_급여일이면_FEED_DAY() {
        assertThat(Mood.of(feed(FeedStatus.DUE), false, TODAY, TODAY)).isEqualTo(Mood.FEED_DAY);
    }

    @Test
    void 탈피한_지_2일_이내면_JUST_SHED() {
        assertThat(Mood.of(feed(FeedStatus.UPCOMING), false, TODAY.minusDays(2), TODAY)).isEqualTo(Mood.JUST_SHED);
        assertThat(Mood.of(feed(FeedStatus.UPCOMING), false, TODAY.minusDays(3), TODAY)).isEqualTo(Mood.NORMAL);
    }

    @Test
    void 해당_없으면_NORMAL() {
        assertThat(Mood.of(feed(FeedStatus.NO_RECORD), false, null, TODAY)).isEqualTo(Mood.NORMAL);
    }

    private static FeedSchedule feed(FeedStatus status) {
        return new FeedSchedule(status, null, null, null);
    }
}
