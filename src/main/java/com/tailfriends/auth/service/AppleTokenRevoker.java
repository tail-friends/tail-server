package com.tailfriends.auth.service;

import com.tailfriends.auth.client.AppleOAuthClient;
import com.tailfriends.auth.repository.UserOAuthTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 애플 로그인 사용자가 탈퇴하면 애플 토큰을 철회한다 (App Store 심사 가이드라인 5.1.1(v)).
 *
 * <p>계정 삭제 트랜잭션이 커밋된 뒤에 철회한다. 롤백되면 철회하지 않는다. 철회가 실패해도 탈퇴는 되돌리지 않고 로그만 남긴다.
 * 토큰 행은 사용자와 함께 ON DELETE CASCADE로 지워지므로, 삭제 전에 복호화해 둔다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppleTokenRevoker {

    private final UserOAuthTokenRepository userOAuthTokenRepository;
    private final OAuthTokenCipher oauthTokenCipher;
    private final AppleOAuthClient appleOAuthClient;

    public void revokeAfterCommit(Long userId) {
        String refreshToken = userOAuthTokenRepository.findRefreshTokenByUserId(userId)
                .map(encrypted -> decryptQuietly(userId, encrypted))
                .orElse(null);
        if (refreshToken == null) {
            log.warn("철회할 애플 토큰이 없어 철회 없이 탈퇴해요: userId={}", userId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                revokeQuietly(userId, refreshToken);
            }
        });
    }

    private String decryptQuietly(Long userId, String encrypted) {
        try {
            return oauthTokenCipher.decrypt(encrypted);
        } catch (RuntimeException e) {
            log.warn("저장된 애플 토큰을 풀지 못해 철회 없이 탈퇴해요: userId={}", userId, e);
            return null;
        }
    }

    private void revokeQuietly(Long userId, String refreshToken) {
        try {
            if (!appleOAuthClient.revoke(refreshToken)) {
                log.warn("애플 토큰 철회에 실패했지만 탈퇴는 완료했어요: userId={}", userId);
            }
        } catch (RuntimeException e) {
            log.warn("애플 토큰 철회에 실패했지만 탈퇴는 완료했어요: userId={}", userId, e);
        }
    }
}
