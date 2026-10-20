package com.tailfriends.health.dto;

import com.tailfriends.health.domain.AlertStatus;
import jakarta.validation.constraints.NotNull;

/**
 * 지금은 ACKNOWLEDGED(확인)로 바꾸는 것만 된다.
 */
public record AlertUpdateRequest(
        @NotNull AlertStatus status
) {
}
