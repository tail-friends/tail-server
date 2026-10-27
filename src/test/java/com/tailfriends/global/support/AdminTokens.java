package com.tailfriends.global.support;

import com.tailfriends.global.security.JwtProvider;
import com.tailfriends.user.domain.Provider;
import com.tailfriends.user.domain.Role;
import com.tailfriends.user.domain.User;
import com.tailfriends.user.repository.UserRepository;
import java.util.UUID;

/**
 * 통합 테스트용. 관리자 API는 DB 역할로 판단하므로(AdminAuthorizationManager) 실제 ADMIN 사용자를 저장하고 토큰을 만든다.
 */
public final class AdminTokens {

    private AdminTokens() {
    }

    /**
     * {@code Authorization} 헤더 값("Bearer ...")을 돌려준다.
     */
    public static String newAdmin(UserRepository userRepository, JwtProvider jwtProvider) {
        User user = User.builder()
                .provider(Provider.KAKAO)
                .providerId(UUID.randomUUID().toString())
                .nickname("관리자")
                .build();
        user.changeRole(Role.ADMIN);
        User saved = userRepository.save(user);
        return "Bearer " + jwtProvider.createAccessToken(saved.getId(), Role.ADMIN);
    }
}
