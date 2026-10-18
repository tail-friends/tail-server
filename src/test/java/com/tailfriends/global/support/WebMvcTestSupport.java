package com.tailfriends.global.support;

import com.tailfriends.global.config.CorsConfig;
import com.tailfriends.global.security.JwtAccessDeniedHandler;
import com.tailfriends.global.security.JwtAuthenticationEntryPoint;
import com.tailfriends.global.security.SecurityConfig;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({SecurityConfig.class, CorsConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
public @interface WebMvcTestSupport {
}
