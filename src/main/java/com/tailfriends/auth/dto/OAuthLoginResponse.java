package com.tailfriends.auth.dto;

/**
 * isNewUser가 true면 앱은 개인정보 동의 화면으로 보낸다.
 * 웹 요청이면 refreshToken은 쿠키로 내려주고 여기서는 null이다.
 */
public record OAuthLoginResponse(
        String accessToken,
        String refreshToken,
        boolean isNewUser,
        LoginUser user
) {

    public OAuthLoginResponse withoutRefreshToken() {
        return new OAuthLoginResponse(accessToken, null, isNewUser, user);
    }

    public record LoginUser(
            Long id,
            String nickname
    ) {
    }
}
