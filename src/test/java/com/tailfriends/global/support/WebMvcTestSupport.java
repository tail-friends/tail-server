package com.tailfriends.global.support;

import com.tailfriends.global.config.ClockConfig;
import com.tailfriends.global.config.CorsConfig;
import com.tailfriends.global.security.JwtAccessDeniedHandler;
import com.tailfriends.global.security.JwtAuthenticationEntryPoint;
import com.tailfriends.global.security.JwtProvider;
import com.tailfriends.global.security.SecurityConfig;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ActiveProfiles("test")
@Import({SecurityConfig.class, CorsConfig.class, ClockConfig.class, JwtProvider.class,
        JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
public @interface WebMvcTestSupport {
}
