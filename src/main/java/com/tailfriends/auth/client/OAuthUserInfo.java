package com.tailfriends.auth.client;

/**
 * 소셜 서비스에서 받은 사용자 정보. nickname과 email은 사용자가 동의하지 않으면 null이다.
 * refreshToken은 탈퇴할 때 철회해야 하는 소셜(지금은 애플)만 준다. 나머지는 null이다.
 */
public record OAuthUserInfo(
        String providerId,
        String nickname,
        String email,
        String refreshToken
) {

    public OAuthUserInfo(String providerId, String nickname, String email) {
        this(providerId, nickname, email, null);
    }

    /**
     * 로그에 찍혀도 refresh token 원문이 나가지 않게 가린다.
     */
    @Override
    public String toString() {
        return "OAuthUserInfo[providerId=" + providerId + ", nickname=" + nickname + ", email=" + email
                + ", refreshToken=" + (refreshToken == null ? null : "****") + "]";
    }
}
