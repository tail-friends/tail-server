package com.tailfriends.auth.dto;

/**
 * pushToken을 보내면 이 기기의 푸시 토큰도 지운다(내 토큰일 때만).
 * 웹은 refreshToken 없이 보내고 쿠키의 리프레시 토큰을 쓴다.
 */
public record LogoutRequest(
        String refreshToken,
        String pushToken
) {
}
