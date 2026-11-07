package com.tailfriends.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * pushToken을 보내면 이 기기의 푸시 토큰도 지운다(내 토큰일 때만).
 */
public record LogoutRequest(
        @NotBlank String refreshToken,
        String pushToken
) {
}
