package com.tailfriends.user.domain;

/**
 * 건강 경고 기준. 기능명세서 4-3의 기본값을 사용자가 바꿀 수 있다.
 *
 * @param refusalCount      "안 먹음"이 몇 번 연속이면 거식 경고
 * @param weightLossPercent 체중이 몇 % 넘게 줄면 체중 감소 경고
 * @param weightLossDays    체중 감소를 며칠 안에서 볼지
 * @param looseStoolCount   묽은 변이 몇 번 연속이면 배변 경고
 */
public record AlertRules(
        int refusalCount,
        int weightLossPercent,
        int weightLossDays,
        int looseStoolCount
) {

    public static final AlertRules DEFAULT = new AlertRules(3, 10, 30, 2);
}
