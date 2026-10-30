package com.tailfriends.health.dto;

/**
 * scheduleId는 투약 일정을 만들지 않았으면 null.
 */
public record VetVisitCreateResponse(Long id, Long scheduleId) {
}
