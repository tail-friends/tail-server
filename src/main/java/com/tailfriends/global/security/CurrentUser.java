package com.tailfriends.global.security;

import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * 지금 요청을 보낸 사용자. 서비스는 컨트롤러에서 사용자 ID를 넘겨받지 않고 이것으로 확인한다.
 * JwtAuthenticationFilter가 principal에 사용자 ID(Long)를 넣는다.
 */
@Component
@RequiredArgsConstructor
public class CurrentUser {

    private final UserRepository userRepository;

    /**
     * 한 요청에서 확인을 마친 사용자 ID를 담아 두는 요청 속성 이름.
     */
    static final String VERIFIED_USER_ID = CurrentUser.class.getName() + ".verifiedUserId";

    /**
     * 인증 정보가 없거나 principal이 사용자 ID가 아니면 UNAUTHORIZED.
     * 탈퇴한 뒤에도 액세스 토큰은 만료 전까지 유효하므로, 사용자가 DB에 없어도 UNAUTHORIZED.
     * 사용자가 있는지는 한 요청에서 한 번만 확인한다. 요청 밖(스케줄러 등)에서 부르면 매번 확인한다.
     */
    public Long id() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        RequestAttributes request = RequestContextHolder.getRequestAttributes();
        if (request != null && userId.equals(request.getAttribute(VERIFIED_USER_ID, RequestAttributes.SCOPE_REQUEST))) {
            return userId;
        }
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (request != null) {
            request.setAttribute(VERIFIED_USER_ID, userId, RequestAttributes.SCOPE_REQUEST);
        }
        return userId;
    }
}
