package com.tailfriends.log.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class LogTest {

    private static final LocalDate DATE = LocalDate.of(2026, 10, 18);

    @Test
    void 종류에_해당하지_않는_값은_저장하지_않는다() {
        LogDetails details = new LogDetails("귀뚜라미", 6, null, true, ShedStage.SHED, true,
                new BigDecimal("52.3"), true, Stool.LOOSE, new BigDecimal("32.0"), null, 40);

        Log log = Log.create(null, null, null, LogType.FEED, DATE, "", details);

        assertThat(log.getFood()).isEqualTo("귀뚜라미");
        assertThat(log.getFoodCount()).isEqualTo(6);
        assertThat(log.getDusted()).isTrue();
        assertThat(log.getShedStage()).isNull();
        assertThat(log.getWeightG()).isNull();
        assertThat(log.getStool()).isNull();
        assertThat(log.getHotC()).isNull();
    }

    @Test
    void 빠진_예_아니오_값은_false로_채운다() {
        Log feed = Log.create(null, null, null, LogType.FEED, DATE, null, details().build());
        Log shed = Log.create(null, null, null, LogType.SHED, DATE, null, details().stage(ShedStage.BLUE).build());
        Log clean = Log.create(null, null, null, LogType.CLEAN, DATE, null, details().build());

        assertThat(feed.getRefused()).isFalse();
        assertThat(feed.getDusted()).isFalse();
        assertThat(shed.getShedComplete()).isFalse();
        assertThat(clean.getCleanFull()).isFalse();
    }

    @Test
    void 탈피_기록은_stage가_없으면_저장할_수_없다() {
        assertInvalidInput(() -> Log.create(null, null, null, LogType.SHED, DATE, null, details().build()));
    }

    @Test
    void 체중_기록은_체중이_없으면_저장할_수_없다() {
        assertInvalidInput(() -> Log.create(null, null, null, LogType.WEIGHT, DATE, null, details().build()));
    }

    @Test
    void 온습도_기록은_온도와_습도가_모두_없으면_저장할_수_없다() {
        assertInvalidInput(() -> Log.create(null, null, null, LogType.ENV, DATE, null, details().build()));

        Log log = Log.create(null, null, null, LogType.ENV, DATE, null, details().humidity(45).build());
        assertThat(log.getHumidity()).isEqualTo(45);
    }

    @Test
    void 메모_기록은_메모가_비어_있으면_저장할_수_없다() {
        assertInvalidInput(() -> Log.create(null, null, null, LogType.NOTE, DATE, "  ", details().build()));
    }

    @Test
    void 수정은_보낸_값만_바꾸고_나머지는_그대로_둔다() {
        Log log = Log.create(null, null, null, LogType.FEED, DATE, "다 먹음",
                details().food("밀웜").count(5).dusted(true).build());

        log.update(null, null, details().count(4).build());

        assertThat(log.getLogDate()).isEqualTo(DATE);
        assertThat(log.getMemo()).isEqualTo("다 먹음");
        assertThat(log.getFood()).isEqualTo("밀웜");
        assertThat(log.getFoodCount()).isEqualTo(4);
        assertThat(log.getDusted()).isTrue();
    }

    @Test
    void 메모_기록의_메모를_비우는_수정은_할_수_없다() {
        Log log = Log.create(null, null, null, LogType.NOTE, DATE, "꼬리 끝 허물", details().build());

        assertInvalidInput(() -> log.update(null, "", details().build()));
    }

    private static void assertInvalidInput(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    private static DetailsBuilder details() {
        return new DetailsBuilder();
    }

    private static final class DetailsBuilder {

        private String food;
        private Integer count;
        private Boolean dusted;
        private ShedStage stage;
        private Integer humidity;

        DetailsBuilder food(String food) {
            this.food = food;
            return this;
        }

        DetailsBuilder count(Integer count) {
            this.count = count;
            return this;
        }

        DetailsBuilder dusted(Boolean dusted) {
            this.dusted = dusted;
            return this;
        }

        DetailsBuilder stage(ShedStage stage) {
            this.stage = stage;
            return this;
        }

        DetailsBuilder humidity(Integer humidity) {
            this.humidity = humidity;
            return this;
        }

        LogDetails build() {
            return new LogDetails(food, count, null, dusted, stage, null, null, null, null, null, null, humidity);
        }
    }
}
