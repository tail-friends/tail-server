package com.tailfriends.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class FeedReminderServiceTest {

    @Test
    void 지금이_속한_10분_구간의_시작을_구한다() {
        assertThat(FeedReminderService.windowStart(LocalTime.of(9, 0))).isEqualTo(LocalTime.of(9, 0));
        assertThat(FeedReminderService.windowStart(LocalTime.of(9, 9, 59))).isEqualTo(LocalTime.of(9, 0));
        assertThat(FeedReminderService.windowStart(LocalTime.of(21, 35, 10))).isEqualTo(LocalTime.of(21, 30));
        assertThat(FeedReminderService.windowStart(LocalTime.of(23, 59))).isEqualTo(LocalTime.of(23, 50));
    }
}
