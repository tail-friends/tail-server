package com.tailfriends.global.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.test.context.support.WithSecurityContext;

/**
 * JwtAuthenticationFilter와 같은 모양(principal = 사용자 ID)의 인증 정보로 테스트한다.
 * 인증이 필요한 API의 컨트롤러 테스트에 {@code @WithMockUser} 대신 이것을 쓴다. 서비스의 CurrentUser가 이 ID를 꺼낸다.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockUserIdSecurityContextFactory.class)
public @interface WithMockUserId {

    long value() default 1L;

    String role() default "KEEPER";
}
