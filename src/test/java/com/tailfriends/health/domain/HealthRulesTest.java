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
}
