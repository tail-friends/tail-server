package com.tailfriends.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.tailfriends.user.domain.Role;
import java.time.Clock;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {

    private final JwtProvider jwtProvider = new JwtProvider(
            new JwtProperties("test-jwt-secret-that-is-at-least-32-bytes-long", Duration.ofMinutes(30), Duration.ofDays(14)),
            Clock.systemUTC());
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtProvider);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 유효한_토큰이면_사용자_ID와_권한으로_인증한다() throws Exception {
        String token = jwtProvider.createAccessToken(7L, Role.KEEPER);

        doFilter("Bearer " + token);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication.getPrincipal()).isEqualTo(7L);
        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_KEEPER");
    }

    @Test
    void 유효하지_않은_토큰이면_인증하지_않는다() throws Exception {
        doFilter("Bearer not-a-jwt");

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void Bearer_헤더가_없으면_인증하지_않는다() throws Exception {
        doFilter(null);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private void doFilter(String authorization) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (authorization != null) {
            request.addHeader(HttpHeaders.AUTHORIZATION, authorization);
        }
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
    }
}
