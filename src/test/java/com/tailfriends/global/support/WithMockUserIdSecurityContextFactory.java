package com.tailfriends.global.support;

import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

public class WithMockUserIdSecurityContextFactory implements WithSecurityContextFactory<WithMockUserId> {

    @Override
    public SecurityContext createSecurityContext(WithMockUserId annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                annotation.value(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + annotation.role()))));
        return context;
    }
}
