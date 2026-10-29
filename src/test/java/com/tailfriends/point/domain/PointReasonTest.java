package com.tailfriends.point.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.tailfriends.log.domain.LogType;
import org.junit.jupiter.api.Test;

class PointReasonTest {

    @Test
    void 급여_체중_탈피_청소_기록은_종류마다_적립_이유가_있다() {
        assertThat(PointReason.ofLog(LogType.FEED)).hasValue(PointReason.LOG_FEED);
        assertThat(PointReason.ofLog(LogType.WEIGHT)).hasValue(PointReason.LOG_WEIGHT);
        assertThat(PointReason.ofLog(LogType.SHED)).hasValue(PointReason.LOG_SHED);
        assertThat(PointReason.ofLog(LogType.CLEAN)).hasValue(PointReason.LOG_CLEAN);
    }

    @Test
    void 온습도와_메모_기록은_적립하지_않는다() {
        assertThat(PointReason.ofLog(LogType.ENV)).isEmpty();
        assertThat(PointReason.ofLog(LogType.NOTE)).isEmpty();
    }
}
