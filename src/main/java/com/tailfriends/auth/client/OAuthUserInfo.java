package com.tailfriends.auth.client;

/**
 * 소셜 서비스에서 받은 사용자 정보. nickname과 email은 사용자가 동의하지 않으면 null이다.
 */
public record OAuthUserInfo(
        String providerId,
        String nickname,
        String email
) {
}
