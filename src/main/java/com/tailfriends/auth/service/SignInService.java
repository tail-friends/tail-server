package com.tailfriends.auth.service;

import com.tailfriends.auth.client.OAuthUserInfo;
import com.tailfriends.auth.domain.UserOAuthToken;
import com.tailfriends.auth.dto.OAuthLoginResponse;
import com.tailfriends.auth.dto.OAuthLoginResponse.LoginUser;
import com.tailfriends.auth.dto.TokenResponse;
import com.tailfriends.auth.repository.UserOAuthTokenRepository;
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
    private final UserOAuthTokenRepository userOAuthTokenRepository;
    private final OAuthTokenCipher oauthTokenCipher;

    /**
     * 처음 보는 소셜 계정이면 가입시키고, 토큰을 발급한다. 기존 사용자의 닉네임·이메일은 바꾸지 않는다.
     * 소셜 refresh token을 받았으면(애플) 탈퇴 때 철회하도록 암호화해 저장한다. 로그인할 때마다 새 토큰으로 바꾼다.
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

        if (userInfo.refreshToken() != null) {
            saveOAuthToken(user, provider, oauthTokenCipher.encrypt(userInfo.refreshToken()));
        }

        TokenResponse tokens = tokenService.issue(user);
        return new OAuthLoginResponse(tokens.accessToken(), tokens.refreshToken(), existing.isEmpty(),
                new LoginUser(user.getId(), user.getNickname()));
    }

    private void saveOAuthToken(User user, Provider provider, String encrypted) {
        userOAuthTokenRepository.findByUserId(user.getId()).ifPresentOrElse(
                saved -> saved.change(encrypted),
                () -> userOAuthTokenRepository.save(UserOAuthToken.of(user, provider, encrypted)));
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
