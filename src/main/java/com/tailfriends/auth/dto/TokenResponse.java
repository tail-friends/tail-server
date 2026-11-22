package com.tailfriends.auth.dto;

/**
 * 웹 요청이면 refreshToken은 쿠키로 내려주고 여기서는 null이다.
 */
public record TokenResponse(
        String accessToken,
        String refreshToken
) {

    public TokenResponse withoutRefreshToken() {
        return new TokenResponse(accessToken, null);
    }
}
