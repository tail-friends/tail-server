package com.tailfriends.auth.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken
) {
}
