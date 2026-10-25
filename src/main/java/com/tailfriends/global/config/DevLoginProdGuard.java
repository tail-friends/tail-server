package com.tailfriends.global.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * 운영 프로필에서 테스트 로그인이 켜져 있으면 서버를 띄우지 않는다.
 * 환경 변수 DEV_LOGIN_ENABLED는 application-prod.yml보다 우선하므로 yml만으로는 막을 수 없다.
 */
@Configuration
@Profile("prod")
@ConditionalOnProperty(name = "dev.login.enabled", havingValue = "true")
public class DevLoginProdGuard {

    public DevLoginProdGuard() {
        throw new IllegalStateException("운영 프로필에서는 테스트 로그인(dev.login.enabled)을 켤 수 없다");
    }
}
