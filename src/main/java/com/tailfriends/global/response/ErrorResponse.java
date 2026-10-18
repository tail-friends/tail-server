package com.tailfriends.global.response;

public record ErrorResponse(
        String code,
        String message
) {
}
