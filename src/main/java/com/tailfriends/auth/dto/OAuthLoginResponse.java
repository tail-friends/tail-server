package com.tailfriends.auth.dto;

/**
 * isNewUser가 true면 앱은 개인정보 동의 화면으로 보낸다.
 */
public record OAuthLoginResponse(
        String accessToken,
        String refreshToken,
        boolean isNewUser,
        LoginUser user
) {

    public record LoginUser(
            Long id,
            String nickname
    ) {
    }
}
