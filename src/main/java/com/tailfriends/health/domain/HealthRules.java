package com.tailfriends.health.domain;

import com.tailfriends.log.domain.Stool;
import com.tailfriends.user.domain.AlertRules;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.OptionalInt;

/**
 * 건강 경고 판정 규칙. 기능명세서 4-3.
 * 기준값은 사용자 설정의 경고 기준(바꾼 적이 없으면 {@link AlertRules#DEFAULT})이다.
 */
public final class HealthRules {

    private HealthRules() {
    }

    /**
     * @param latestRefused 최근 급여 기록의 "안 먹음" 여부, 최신순. refusalCount개까지 본다.
     */
    public static boolean isRefusalStreak(AlertRules rules, List<Boolean> latestRefused) {
        return latestRefused.size() >= rules.refusalCount()
                && latestRefused.stream().limit(rules.refusalCount()).allMatch(Boolean.TRUE::equals);
    }

    /**
     * 기간 안 최고 체중보다 최근 체중이 weightLossPercent를 넘게 줄었으면 감소율(%, 버림)을 돌려준다.
     *
     * @param latest  가장 최근 체중
     * @param highest 최근 체중 기록일로부터 weightLossDays일 안의 최고 체중 (최근 체중 포함)
     */
    public static OptionalInt weightLossPercent(AlertRules rules, BigDecimal latest, BigDecimal highest) {
        if (latest == null || highest == null || highest.signum() <= 0) {
            return OptionalInt.empty();
        }
        BigDecimal lossPercent = highest.subtract(latest)
                .multiply(BigDecimal.valueOf(100))
                .divide(highest, 2, RoundingMode.DOWN);
        if (lossPercent.compareTo(BigDecimal.valueOf(rules.weightLossPercent())) <= 0) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(lossPercent.setScale(0, RoundingMode.DOWN).intValue());
    }

    /**
     * @param latestStools 배변이 있었던(정상·묽음) 최근 청소 기록의 배변 상태, 최신순. "없음"은 넣지 않는다.
     */
    public static boolean isLooseStoolStreak(AlertRules rules, List<Stool> latestStools) {
        return latestStools.size() >= rules.looseStoolCount()
                && latestStools.stream().limit(rules.looseStoolCount()).allMatch(Stool.LOOSE::equals);
    }

    public static String refusalMessage(AlertRules rules) {
        return "최근 %d번 연속으로 먹지 않았어요".formatted(rules.refusalCount());
    }

    public static String weightLossMessage(AlertRules rules, int percent) {
        return "최근 %d일 동안 체중이 %d%% 줄었어요".formatted(rules.weightLossDays(), percent);
    }

    public static String looseStoolMessage(AlertRules rules) {
        return "최근 %d번 연속으로 묽은 변을 봤어요".formatted(rules.looseStoolCount());
    }
}
