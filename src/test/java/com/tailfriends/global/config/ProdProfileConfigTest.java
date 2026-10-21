package com.tailfriends.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

class ProdProfileConfigTest {

    @Test
    void 운영_프로필은_환경_변수와_상관없이_테스트_로그인을_끈다() throws Exception {
        List<PropertySource<?>> sources = new YamlPropertySourceLoader()
                .load("prod", new ClassPathResource("application-prod.yml"));

        assertThat(sources.getFirst().getProperty("dev.login.enabled")).isEqualTo(false);
    }
}
