package com.tailfriends.health.dto;

import com.tailfriends.health.domain.AlertStatus;
import com.tailfriends.health.domain.AlertType;
import com.tailfriends.health.domain.HealthAlert;
import java.time.Instant;

public record AlertResponse(
        Long id,
        Long animalId,
        AlertType type,
        String message,
        AlertStatus status,
        Instant createdAt
) {

    public static AlertResponse from(HealthAlert alert) {
        return new AlertResponse(
                alert.getId(),
                alert.getAnimal().getId(),
                alert.getType(),
                alert.getMessage(),
                alert.getStatus(),
                alert.getCreatedAt());
    }
}
