package com.tailfriends.auth.client;

import com.tailfriends.user.domain.Provider;

/**
 * 소셜 로그인 방식 하나. 인가 코드로 그 서비스의 사용자 정보를 가져온다.
 */
public interface OAuthClient {

    Provider provider();

    /**
     * @throws com.tailfriends.global.exception.BusinessException 인가 코드가 틀리거나 만료되면 OAUTH_FAILED
     */
    OAuthUserInfo fetchUser(String code, String redirectUri);
}
