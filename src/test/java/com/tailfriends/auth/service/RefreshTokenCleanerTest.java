package com.tailfriends.auth.service;

import static org.mockito.Mockito.verify;

import com.tailfriends.auth.repository.RefreshTokenRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefreshTokenCleanerTest {

    private static final Instant NOW = Instant.parse("2026-10-27T19:30:00Z");

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    void 지금_시각을_기준으로_만료된_토큰을_지운다() {
        new RefreshTokenCleaner(refreshTokenRepository, Clock.fixed(NOW, ZoneOffset.UTC)).deleteExpired();

        verify(refreshTokenRepository).deleteExpired(NOW);
    }
}
