package com.tailfriends.health.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.tailfriends.log.domain.Stool;
import com.tailfriends.user.domain.AlertRules;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class HealthRulesTest {

    private static final AlertRules DEFAULT = AlertRules.DEFAULT;

    @Test
    void 안_먹음이_3번_연속이면_거식이다() {
        assertThat(HealthRules.isRefusalStreak(DEFAULT, List.of(true, true, true))).isTrue();
    }

    @Test
    void 중간에_한_번이라도_먹었으면_거식이_아니다() {
        assertThat(HealthRules.isRefusalStreak(DEFAULT, List.of(true, false, true))).isFalse();
        assertThat(HealthRules.isRefusalStreak(DEFAULT, List.of(false, true, true))).isFalse();
    }

    @Test
    void 급여_기록이_3개보다_적으면_거식이_아니다() {
        assertThat(HealthRules.isRefusalStreak(DEFAULT, List.of(true, true))).isFalse();
    }

    @Test
    void 체중이_10퍼센트를_넘게_줄면_감소율을_버림으로_돌려준다() {
        assertThat(HealthRules.weightLossPercent(DEFAULT, new BigDecimal("50.0"), new BigDecimal("57.0")))
                .hasValue(12);
    }

    @Test
    void 체중이_정확히_10퍼센트_줄면_경고하지_않는다() {
        assertThat(HealthRules.weightLossPercent(DEFAULT, new BigDecimal("45.0"), new BigDecimal("50.0"))).isEmpty();
    }

    @Test
    void 체중이_늘었거나_비교할_기록이_없으면_경고하지_않는다() {
        assertThat(HealthRules.weightLossPercent(DEFAULT, new BigDecimal("52.0"), new BigDecimal("52.0"))).isEmpty();
        assertThat(HealthRules.weightLossPercent(DEFAULT, new BigDecimal("52.0"), null)).isEmpty();
    }

    @Test
    void 묽은_변이_2번_연속이면_경고한다() {
        assertThat(HealthRules.isLooseStoolStreak(DEFAULT, List.of(Stool.LOOSE, Stool.LOOSE))).isTrue();
        assertThat(HealthRules.isLooseStoolStreak(DEFAULT, List.of(Stool.LOOSE, Stool.NORMAL))).isFalse();
        assertThat(HealthRules.isLooseStoolStreak(DEFAULT, List.of(Stool.LOOSE))).isFalse();
    }

    @Test
    void 사용자가_바꾼_기준으로_판정한다() {
        AlertRules strict = new AlertRules(2, 5, 14, 3);

        assertThat(HealthRules.isRefusalStreak(strict, List.of(true, true))).isTrue();
        assertThat(HealthRules.weightLossPercent(strict, new BigDecimal("47.0"), new BigDecimal("50.0"))).hasValue(6);
        assertThat(HealthRules.isLooseStoolStreak(strict, List.of(Stool.LOOSE, Stool.LOOSE))).isFalse();
    }

    @Test
    void 경고_문구에_기준과_감소율을_넣는다() {
        assertThat(HealthRules.weightLossMessage(DEFAULT, 12)).isEqualTo("최근 30일 동안 체중이 12% 줄었어요");
        assertThat(HealthRules.refusalMessage(DEFAULT)).isEqualTo("최근 3번 연속으로 먹지 않았어요");
        assertThat(HealthRules.weightLossMessage(new AlertRules(3, 5, 14, 2), 6))
                .isEqualTo("최근 14일 동안 체중이 6% 줄었어요");
    }

    @Test
    void 온습도가_모두_범위_안이면_환경_경고가_없다() {
        assertThat(HealthRules.envWarningMessage(List.of(
                hot("31.0", "31", "35"), cool("24", "22", "26"), EnvReading.humidity(40, 30, 40)))).isEmpty();
    }

    @Test
    void 한_항목만_벗어나면_어느_쪽으로_벗어났는지와_범위를_적는다() {
        assertThat(HealthRules.envWarningMessage(List.of(hot("29.5", "31", "35"))))
                .hasValue("핫존 온도가 적정 범위(31~35℃)보다 낮아요");
        assertThat(HealthRules.envWarningMessage(List.of(EnvReading.humidity(55, 30, 40))))
                .hasValue("습도가 적정 범위(30~40%)보다 높아요");
        assertThat(HealthRules.envWarningMessage(List.of(cool("21.5", "22.5", "26"))))
                .hasValue("쿨존 온도가 적정 범위(22.5~26℃)보다 낮아요");
    }

    @Test
    void 여러_항목이_벗어나면_항목만_이어서_적는다() {
        assertThat(HealthRules.envWarningMessage(List.of(
                hot("36", "31", "35"), cool("24", "22", "26"), EnvReading.humidity(20, 30, 40))))
                .hasValue("핫존 온도와 습도가 적정 범위를 벗어났어요");
        assertThat(HealthRules.envWarningMessage(List.of(
                hot("36", "31", "35"), cool("20", "22", "26"), EnvReading.humidity(20, 30, 40))))
                .hasValue("핫존 온도, 쿨존 온도와 습도가 적정 범위를 벗어났어요");
    }

    @Test
    void 기록에_값이_없거나_종에_범위가_없는_항목은_보지_않는다() {
        assertThat(HealthRules.envWarningMessage(List.of(
                hot(null, "31", "35"), cool("10", null, null), EnvReading.humidity(90, null, null)))).isEmpty();
    }

    @Test
    void 범위가_한쪽만_있으면_그쪽만_보고_이상_이하로_적는다() {
        assertThat(HealthRules.envWarningMessage(List.of(hot("30", "31", null))))
                .hasValue("핫존 온도가 적정 범위(31℃ 이상)보다 낮아요");
        assertThat(HealthRules.envWarningMessage(List.of(EnvReading.humidity(70, null, 60))))
                .hasValue("습도가 적정 범위(60% 이하)보다 높아요");
        assertThat(HealthRules.envWarningMessage(List.of(hot("50", "31", null)))).isEmpty();
    }

    @Test
    void 경계값은_범위_안이다() {
        assertThat(HealthRules.envWarningMessage(List.of(hot("31", "31", "35"), hot("35.0", "31", "35")))).isEmpty();
    }

    private static EnvReading hot(String value, String min, String max) {
        return new EnvReading("핫존 온도", decimal(value), decimal(min), decimal(max), "℃");
    }

    private static EnvReading cool(String value, String min, String max) {
        return new EnvReading("쿨존 온도", decimal(value), decimal(min), decimal(max), "℃");
    }

    private static BigDecimal decimal(String value) {
        return value == null ? null : new BigDecimal(value);
    }
}
