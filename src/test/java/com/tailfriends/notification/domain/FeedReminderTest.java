package com.tailfriends.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class FeedReminderTest {

    @Test
    void 오늘_급여일인_개체만_있으면_밥_주는_날이라고_알린다() {
        assertThat(FeedReminder.body(List.of("망고", "초코"), List.of()))
                .hasValue("오늘 망고, 초코 밥 주는 날이에요");
    }

    @Test
    void 이틀_지난_개체만_있으면_급여일이_지났다고_알린다() {
        assertThat(FeedReminder.body(List.of(), List.of("망고")))
                .hasValue("망고 급여일이 2일 지났어요");
    }

    @Test
    void 둘_다_있으면_한_알림으로_묶는다() {
        assertThat(FeedReminder.body(List.of("망고"), List.of("초코")))
                .hasValue("오늘 망고 밥 주는 날이에요. 초코 급여일이 2일 지났어요");
    }

    @Test
    void 보낼_개체가_없으면_알리지_않는다() {
        assertThat(FeedReminder.body(List.of(), List.of())).isEmpty();
    }

    @Test
    void 네_마리부터는_세_마리_이름과_나머지_수를_쓴다() {
        assertThat(FeedReminder.names(List.of("망고", "초코", "보리", "콩이", "두부")))
                .isEqualTo("망고, 초코, 보리 외 2마리");
    }
}
