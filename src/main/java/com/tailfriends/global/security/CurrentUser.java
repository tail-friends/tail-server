package com.tailfriends.global.security;

import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 지금 요청을 보낸 사용자. 서비스는 컨트롤러에서 사용자 ID를 넘겨받지 않고 이것으로 확인한다.
 * JwtAuthenticationFilter가 principal에 사용자 ID(Long)를 넣는다.
 */
@Component
public class CurrentUser {

    /**
     * 인증 정보가 없거나 principal이 사용자 ID가 아니면 UNAUTHORIZED.
     */
    public Long id() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }
}
