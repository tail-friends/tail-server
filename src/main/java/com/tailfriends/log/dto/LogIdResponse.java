package com.tailfriends.log.dto;

/**
 * 이미 저장된 기록의 ID. 409 DUPLICATE_CLIENT_ID 응답의 data로 쓴다.
 */
public record LogIdResponse(
        Long id
) {
}
