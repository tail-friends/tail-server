package com.tailfriends.log.repository;

import com.tailfriends.log.domain.LogType;

public record DoneLog(
        Long logId,
        Long animalId,
        LogType type
) {
}
