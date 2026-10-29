package com.tailfriends.health.domain;

import java.math.BigDecimal;

/**
 * 온습도 기록의 한 항목(핫존 온도, 쿨존 온도, 습도)과 종의 적정 범위.
 * 값이 없거나 범위가 하나도 없으면 판정하지 않는다. 경계값은 범위 안이다.
 *
 * @param label 문구에 쓰는 이름. 받침 없는 말로 끝나야 한다("~가", "~와")
 */
public record EnvReading(String label, BigDecimal value, BigDecimal min, BigDecimal max, String unit) {

    public static EnvReading humidity(Integer value, Integer min, Integer max) {
        return new EnvReading("습도", decimal(value), decimal(min), decimal(max), "%");
    }

    public boolean isJudgeable() {
        return value != null && (min != null || max != null);
    }

    public boolean isBelow() {
        return isJudgeable() && min != null && value.compareTo(min) < 0;
    }

    public boolean isAbove() {
        return isJudgeable() && max != null && value.compareTo(max) > 0;
    }

    public boolean isOutOfRange() {
        return isBelow() || isAbove();
    }

    /**
     * "31~35℃", 최소만 있으면 "31℃ 이상", 최대만 있으면 "35℃ 이하".
     */
    public String rangeText() {
        if (min != null && max != null) {
            return plain(min) + "~" + plain(max) + unit;
        }
        return min != null ? plain(min) + unit + " 이상" : plain(max) + unit + " 이하";
    }

    private static String plain(BigDecimal number) {
        return number.stripTrailingZeros().toPlainString();
    }

    private static BigDecimal decimal(Integer number) {
        return number == null ? null : BigDecimal.valueOf(number);
    }
}
