package com.tailfriends.photo.dto;

/**
 * 성장 앨범 사진의 건강 체크 요약. attentionCount는 "확인 필요" 항목 수다.
 */
public record HealthCheckSummary(Long checkId, int attentionCount) {
}
