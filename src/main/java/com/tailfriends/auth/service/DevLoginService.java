package com.tailfriends.auth.service;

import com.tailfriends.auth.dto.DevLoginRequest;
import com.tailfriends.auth.dto.DevLoginResponse;
import com.tailfriends.auth.dto.TokenResponse;
import com.tailfriends.user.domain.Provider;
import com.tailfriends.user.domain.User;
import com.tailfriends.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로컬 테스트 로그인. dev.login.enabled=true일 때만 빈이 생긴다. (운영 프로필은 항상 false)
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "dev.login.enabled", havingValue = "true")
public class DevLoginService {

    private final UserRepository userRepository;
    private final TokenService tokenService;

    @Transactional
    public DevLoginResponse login(DevLoginRequest request) {
        String name = request.name().strip();
        User user = userRepository.findByProviderAndProviderId(Provider.DEV, name)
                .orElseGet(() -> userRepository.save(User.builder()
                        .provider(Provider.DEV)
                        .providerId(name)
                        .nickname(name)
                        .build()));
        if (request.role() != null) {
            user.changeRole(request.role());
        }
        TokenResponse tokens = tokenService.issue(user);
        return new DevLoginResponse(user.getId(), user.getRole(), tokens.accessToken(), tokens.refreshToken());
    }
}
