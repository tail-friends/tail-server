package com.tailfriends.auth.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 소셜 로그인 앱 키. 값은 환경 변수로만 넣는다. 비어 있으면 그 방식으로는 로그인할 수 없다.
 */
@ConfigurationProperties(prefix = "oauth")
public record OAuthProperties(
        Registration kakao,
        Registration google
) {

    public record Registration(
            String clientId,
            String clientSecret
    ) {
    }
}
