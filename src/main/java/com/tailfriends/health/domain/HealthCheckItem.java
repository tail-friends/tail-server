package com.tailfriends.health.domain;

/**
 * 건강 체크 항목 하나의 결과. note는 사용자에게 보여 줄 짧은 설명이고 없을 수 있다.
 */
public record HealthCheckItem(
        HealthCheckKey key,
        HealthCheckResult result,
        String note
) {

    public boolean needsAttention() {
        return result == HealthCheckResult.ATTENTION;
    }
}
