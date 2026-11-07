package com.tailfriends.health.domain;

public enum HealthCheckResult {
    GOOD,
    ATTENTION,
    /** 사진으로 판단할 수 없음 */
    UNKNOWN
}
