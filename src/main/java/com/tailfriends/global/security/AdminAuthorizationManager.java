package com.tailfriends.global.security;

import com.tailfriends.user.domain.Role;
import com.tailfriends.user.repository.UserRepository;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

/**
 * 관리자 경로의 권한을 토큰의 역할이 아니라 요청 때의 DB 역할로 판단한다.
 * 토큰의 역할은 발급할 때 값이라, 역할을 바꿔도 토큰이 만료될 때까지(최대 30분) 남는다.
 * 거부하면 로그인하지 않은 요청은 401, 로그인한 요청은 403이 된다.
 */
@Component
@RequiredArgsConstructor
public class AdminAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final UserRepository userRepository;

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext context) {
        return new AuthorizationDecision(isAdmin(authentication.get()));
    }

    private boolean isAdmin(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
            return false;
        }
        return userRepository.findRoleById(userId).filter(Role.ADMIN::equals).isPresent();
    }
}
