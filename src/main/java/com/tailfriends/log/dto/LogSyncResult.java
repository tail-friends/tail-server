package com.tailfriends.log.dto;

import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.response.ErrorResponse;
import java.util.UUID;

/**
 * 항목 하나의 처리 결과. CREATED·DUPLICATE면 id(새 기록 또는 기존 기록), FAILED면 error.
 */
public record LogSyncResult(
        UUID clientId,
        Status status,
        Long id,
        ErrorResponse error
) {

    public enum Status {
        CREATED, DUPLICATE, FAILED
    }

    public static LogSyncResult created(UUID clientId, Long id) {
        return new LogSyncResult(clientId, Status.CREATED, id, null);
    }

    public static LogSyncResult duplicate(UUID clientId, Long id) {
        return new LogSyncResult(clientId, Status.DUPLICATE, id, null);
    }

    public static LogSyncResult failed(UUID clientId, ErrorCode code, String message) {
        return new LogSyncResult(clientId, Status.FAILED, null, new ErrorResponse(code.name(), message));
    }
}
