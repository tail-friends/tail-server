package com.tailfriends.auth.service;

import com.tailfriends.auth.domain.RefreshToken;
import com.tailfriends.auth.dto.TokenResponse;
import com.tailfriends.auth.repository.RefreshTokenRepository;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.security.CurrentUser;
import com.tailfriends.global.security.JwtProperties;
import com.tailfriends.global.security.JwtProvider;
import com.tailfriends.user.domain.User;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private static final int REFRESH_TOKEN_BYTES = 32;

    /**
     * 쓴 토큰이 이 시간 안에 다시 오면 앱의 재시도나 동시 요청으로 보고 묶음을 지우지 않는다.
     */
    static final Duration REUSE_GRACE = Duration.ofSeconds(10);

    private final CurrentUser currentUser;
    private final SecureRandom secureRandom = new SecureRandom();

    private final JwtProvider jwtProvider;
    private final JwtProperties jwtProperties;
    private final RefreshTokenRepository refreshTokenRepository;
    private final Clock clock;

    /**
     * 로그인할 때 부른다. 새 토큰 묶음으로 액세스 토큰과 리프레시 토큰을 발급한다. 리프레시 토큰은 원문 대신 SHA-256 해시만 저장한다.
     */
    @Transactional
    public TokenResponse issue(User user) {
        return issue(user, UUID.randomUUID());
    }

    /**
     * 리프레시 토큰으로 토큰을 다시 발급한다. 쓴 토큰은 지우지 않고 쓴 시각을 남기며, 새 토큰은 같은 묶음을 이어받는다.
     * 없거나 만료되었거나 이미 쓴 토큰이면 INVALID_REFRESH_TOKEN.
     * 쓴 지 REUSE_GRACE가 지난 토큰이 다시 오면 도난으로 보고 그 묶음을 모두 지운다. 지운 것은 예외를 던져도 롤백하지 않는다.
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public TokenResponse reissue(String refreshToken) {
        Instant now = clock.instant();
        RefreshToken saved = refreshTokenRepository.findByTokenHash(RefreshToken.hash(refreshToken))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));
        if (saved.isExpired(now)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        if (saved.isUsed()) {
            throw rejectReuse(saved, now);
        }
        if (refreshTokenRepository.markUsed(saved.getId(), now) == 0) {
            // 같은 토큰으로 동시에 들어온 다른 요청이 먼저 썼다
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        return issue(saved.getUser(), saved.getFamilyId());
    }

    /**
     * 내 리프레시 토큰의 묶음(같은 로그인에서 재발급한 토큰들)을 지운다. 없거나 남의 토큰이면 아무것도 하지 않는다.
     */
    @Transactional
    public void revoke(String refreshToken) {
        Long userId = currentUser.id();
        refreshTokenRepository.deleteFamilyByTokenHashAndUserId(RefreshToken.hash(refreshToken), userId);
    }

    private TokenResponse issue(User user, UUID familyId) {
        String accessToken = jwtProvider.createAccessToken(user.getId(), user.getRole());
        String refreshToken = generateRefreshToken();
        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .tokenHash(RefreshToken.hash(refreshToken))
                .expiresAt(clock.instant().plus(jwtProperties.refreshTokenExpiration()))
                .familyId(familyId)
                .build());
        return new TokenResponse(accessToken, refreshToken);
    }

    /**
     * 쓴 지 얼마 안 됐으면 묶음은 두고, 아니면 묶음을 지운다. 어느 쪽이든 INVALID_REFRESH_TOKEN.
     */
    private BusinessException rejectReuse(RefreshToken saved, Instant now) {
        if (!saved.isUsedWithin(REUSE_GRACE, now)) {
            int deleted = refreshTokenRepository.deleteByFamilyId(saved.getFamilyId());
            log.warn("쓴 리프레시 토큰이 다시 와서 토큰 묶음을 지웠다. userId={}, deleted={}",
                    saved.getUser().getId(), deleted);
        }
        return new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    private String generateRefreshToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
