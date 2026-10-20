package com.tailfriends.health.domain;

/**
 * 건강 경고 종류. 기능명세서 4-3.
 */
public enum AlertType {
    /** "안 먹음" 연속 */
    REFUSAL_ALERT,
    /** 최근 기간 안 체중 급감 */
    WEIGHT_LOSS_ALERT,
    /** 묽은 변 연속 */
    STOOL_ALERT,
    /** 온습도가 적정 범위 밖 (종별 기준이 정해지면 판정) */
    ENV_WARNING,
    /** 사진 건강 체크에서 "확인 필요" */
    HEALTH_CHECK_ATTENTION
}
