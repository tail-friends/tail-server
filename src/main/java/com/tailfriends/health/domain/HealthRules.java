package com.tailfriends.health.domain;

import com.tailfriends.log.domain.Stool;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.OptionalInt;

/**
 * 건강 경고 판정 규칙. 기능명세서 4-3.
 * 기준값은 user_settings의 기본값과 같다. 설정 API가 생기면 사용자별 값으로 바꾼다.
 */
public final class HealthRules {

    public static final int REFUSAL_COUNT = 3;
    public static final int WEIGHT_LOSS_PERCENT = 10;
    public static final int WEIGHT_LOSS_DAYS = 30;
    public static final int LOOSE_STOOL_COUNT = 2;

    private HealthRules() {
    }

    /**
     * @param latestRefused 최근 급여 기록의 "안 먹음" 여부, 최신순. REFUSAL_COUNT개까지 본다.
     */
    public static boolean isRefusalStreak(List<Boolean> latestRefused) {
        return latestRefused.size() >= REFUSAL_COUNT
                && latestRefused.stream().limit(REFUSAL_COUNT).allMatch(Boolean.TRUE::equals);
    }

    /**
     * 기간 안 최고 체중보다 최근 체중이 WEIGHT_LOSS_PERCENT를 넘게 줄었으면 감소율(%, 버림)을 돌려준다.
     *
     * @param latest  가장 최근 체중
     * @param highest 최근 체중 기록일로부터 WEIGHT_LOSS_DAYS일 안의 최고 체중 (최근 체중 포함)
     */
    public static OptionalInt weightLossPercent(BigDecimal latest, BigDecimal highest) {
        if (latest == null || highest == null || highest.signum() <= 0) {
            return OptionalInt.empty();
        }
        BigDecimal lossPercent = highest.subtract(latest)
                .multiply(BigDecimal.valueOf(100))
                .divide(highest, 2, RoundingMode.DOWN);
        if (lossPercent.compareTo(BigDecimal.valueOf(WEIGHT_LOSS_PERCENT)) <= 0) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(lossPercent.setScale(0, RoundingMode.DOWN).intValue());
    }

    /**
     * @param latestStools 배변이 있었던(정상·묽음) 최근 청소 기록의 배변 상태, 최신순. "없음"은 넣지 않는다.
     */
    public static boolean isLooseStoolStreak(List<Stool> latestStools) {
        return latestStools.size() >= LOOSE_STOOL_COUNT
                && latestStools.stream().limit(LOOSE_STOOL_COUNT).allMatch(Stool.LOOSE::equals);
    }

    public static String refusalMessage() {
        return "최근 %d번 연속으로 먹지 않았어요".formatted(REFUSAL_COUNT);
    }

    public static String weightLossMessage(int percent) {
        return "최근 %d일 동안 체중이 %d%% 줄었어요".formatted(WEIGHT_LOSS_DAYS, percent);
    }

    public static String looseStoolMessage() {
        return "최근 %d번 연속으로 묽은 변을 봤어요".formatted(LOOSE_STOOL_COUNT);
    }
}
