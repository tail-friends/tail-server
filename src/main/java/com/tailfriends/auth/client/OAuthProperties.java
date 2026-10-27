package com.tailfriends.auth.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 소셜 로그인 앱 키. 값은 환경 변수로만 넣는다. 비어 있으면 그 방식으로는 로그인할 수 없다.
 */
@ConfigurationProperties(prefix = "oauth")
public record OAuthProperties(
        Registration kakao,
        Registration google,
        Apple apple
) {

    public record Registration(
            String clientId,
            String clientSecret
    ) {
    }

    /**
     * 애플은 고정 client secret 대신 서명 키로 매번 client secret(JWT)을 만든다.
     *
     * @param clientId   iOS 앱 번들 ID
     * @param teamId     애플 개발자 팀 ID
     * @param keyId      Sign in with Apple 키 ID
     * @param privateKey 그 키의 .p8 파일 내용 (PEM 또는 BEGIN/END 줄을 뺀 Base64)
     */
    public record Apple(
            String clientId,
            String teamId,
            String keyId,
            String privateKey
    ) {
    }
}
