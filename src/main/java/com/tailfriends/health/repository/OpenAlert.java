package com.tailfriends.health.repository;

import com.tailfriends.health.domain.AlertType;

public record OpenAlert(
        Long id,
        Long animalId,
        AlertType type
) {
}
