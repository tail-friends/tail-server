package com.tailfriends.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

class ProdProfileConfigTest {

    @Test
    void 운영_프로필은_기본으로_테스트_로그인을_끈다() {
        try (ConfigurableApplicationContext context = run(Map.of(), "prod")) {
            assertThat(context.getEnvironment().getProperty("dev.login.enabled")).isEqualTo("false");
        }
    }

    @Test
    void 운영_프로필에서_환경_변수로_테스트_로그인을_켜면_서버가_뜨지_않는다() {
        assertThatThrownBy(() -> run(Map.of("DEV_LOGIN_ENABLED", "true"), "prod"))
                .rootCause()
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("dev.login.enabled");
    }

    @Test
    void 로컬_프로필에서는_환경_변수로_테스트_로그인을_켤_수_있다() {
        try (ConfigurableApplicationContext context = run(Map.of("DEV_LOGIN_ENABLED", "true"), "local")) {
            assertThat(context.getEnvironment().getProperty("dev.login.enabled")).isEqualTo("true");
        }
    }

    /** 실제 설정 파일(application*.yml)과 주어진 환경 변수로 가드만 올려 본다. */
    private static ConfigurableApplicationContext run(Map<String, Object> envVars, String profile) {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().replace(
                StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                new SystemEnvironmentPropertySource(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, envVars));
        return new SpringApplicationBuilder(DevLoginProdGuard.class)
                .web(WebApplicationType.NONE)
                .environment(environment)
                .profiles(profile)
                .run();
    }
}
