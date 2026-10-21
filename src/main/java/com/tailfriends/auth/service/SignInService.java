package com.tailfriends.auth.service;

import com.tailfriends.auth.client.OAuthUserInfo;
import com.tailfriends.auth.dto.OAuthLoginResponse;
import com.tailfriends.auth.dto.OAuthLoginResponse.LoginUser;
import com.tailfriends.auth.dto.TokenResponse;
import com.tailfriends.user.domain.Provider;
import com.tailfriends.user.domain.User;
import com.tailfriends.user.repository.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SignInService {

    static final String DEFAULT_NICKNAME = "사육사";
    private static final int NICKNAME_MAX_LENGTH = 30;

    private final UserRepository userRepository;
    private final TokenService tokenService;

    /**
     * 처음 보는 소셜 계정이면 가입시키고, 토큰을 발급한다. 기존 사용자의 닉네임·이메일은 바꾸지 않는다.
     */
    @Transactional
    public OAuthLoginResponse signIn(Provider provider, OAuthUserInfo userInfo) {
        Optional<User> existing = userRepository.findByProviderAndProviderId(provider, userInfo.providerId());
        User user = existing.orElseGet(() -> userRepository.save(User.builder()
                .provider(provider)
                .providerId(userInfo.providerId())
                .nickname(nicknameOf(userInfo.nickname()))
                .email(userInfo.email())
                .build()));

        TokenResponse tokens = tokenService.issue(user);
        return new OAuthLoginResponse(tokens.accessToken(), tokens.refreshToken(), existing.isEmpty(),
                new LoginUser(user.getId(), user.getNickname()));
    }

    /**
     * 닉네임 동의를 안 했으면 기본 닉네임. 길면 30자(DB 길이)까지만 쓴다.
     */
    private static String nicknameOf(String nickname) {
        if (!StringUtils.hasText(nickname)) {
            return DEFAULT_NICKNAME;
        }
        String stripped = nickname.strip();
        if (stripped.codePointCount(0, stripped.length()) <= NICKNAME_MAX_LENGTH) {
            return stripped;
        }
        return stripped.substring(0, stripped.offsetByCodePoints(0, NICKNAME_MAX_LENGTH));
    }
}
