package com.tailfriends.auth.service;

import com.tailfriends.auth.domain.RefreshToken;
import com.tailfriends.auth.dto.TokenResponse;
import com.tailfriends.auth.repository.RefreshTokenRepository;
import com.tailfriends.global.security.JwtProperties;
import com.tailfriends.global.security.JwtProvider;
import com.tailfriends.user.domain.User;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TokenService {

    private static final int REFRESH_TOKEN_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();

    private final JwtProvider jwtProvider;
    private final JwtProperties jwtProperties;
    private final RefreshTokenRepository refreshTokenRepository;
    private final Clock clock;

    /**
     * 액세스 토큰과 리프레시 토큰을 발급한다. 리프레시 토큰은 원문 대신 SHA-256 해시만 저장한다.
     */
    @Transactional
    public TokenResponse issue(User user) {
        String accessToken = jwtProvider.createAccessToken(user.getId(), user.getRole());
        String refreshToken = generateRefreshToken();
        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .tokenHash(RefreshToken.hash(refreshToken))
                .expiresAt(clock.instant().plus(jwtProperties.refreshTokenExpiration()))
                .build());
        return new TokenResponse(accessToken, refreshToken);
    }

    private String generateRefreshToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
