package com.tailfriends.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.tailfriends.auth.client.AppleOAuthClient;
import com.tailfriends.auth.repository.UserOAuthTokenRepository;
import java.util.Base64;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class AppleTokenRevokerTest {

    private final UserOAuthTokenRepository repository = mock(UserOAuthTokenRepository.class);
    private final OAuthTokenCipher cipher = new OAuthTokenCipher(Base64.getEncoder().encodeToString(new byte[32]));
    private final AppleOAuthClient appleOAuthClient = mock(AppleOAuthClient.class);
    private final AppleTokenRevoker revoker = new AppleTokenRevoker(repository, cipher, appleOAuthClient);

    @BeforeEach
    void 트랜잭션_동기화_시작() {
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void 트랜잭션_동기화_정리() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void 커밋되기_전에는_철회하지_않고_커밋된_뒤에_원문으로_철회한다() {
        given(repository.findRefreshTokenByUserId(1L)).willReturn(Optional.of(cipher.encrypt("apple-refresh")));

        revoker.revokeAfterCommit(1L);
        verify(appleOAuthClient, never()).revoke(any());

        commit();
        verify(appleOAuthClient).revoke("apple-refresh");
    }

    @Test
    void 저장된_토큰이_없으면_철회를_예약하지_않는다() {
        given(repository.findRefreshTokenByUserId(1L)).willReturn(Optional.empty());

        revoker.revokeAfterCommit(1L);

        assertThat(TransactionSynchronizationManager.getSynchronizations()).isEmpty();
    }

    @Test
    void 토큰을_풀지_못하면_철회를_예약하지_않는다() {
        given(repository.findRefreshTokenByUserId(1L)).willReturn(Optional.of("broken"));

        assertThatCode(() -> revoker.revokeAfterCommit(1L)).doesNotThrowAnyException();
        assertThat(TransactionSynchronizationManager.getSynchronizations()).isEmpty();
    }

    @Test
    void 철회_중_예외가_나도_밖으로_던지지_않는다() {
        given(repository.findRefreshTokenByUserId(1L)).willReturn(Optional.of(cipher.encrypt("apple-refresh")));
        given(appleOAuthClient.revoke("apple-refresh")).willThrow(new IllegalStateException("애플 키 없음"));

        revoker.revokeAfterCommit(1L);

        assertThatCode(this::commit).doesNotThrowAnyException();
    }

    private void commit() {
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
    }
}
