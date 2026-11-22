package com.tailfriends.auth.dto;

/**
 * 웹은 본문 없이 보내고 쿠키의 리프레시 토큰을 쓴다. 앱은 refreshToken을 보낸다.
 */
public record TokenReissueRequest(
        String refreshToken
) {
}
