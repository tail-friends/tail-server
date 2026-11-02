package com.tailfriends.photo.dto;

/**
 * 성장 앨범 사진의 건강 체크 요약. 사진 건강 체크(F-33)가 생길 때까지 앨범에서는 항상 null이다.
 */
public record HealthCheckSummary(Long checkId, int attentionCount) {
}
