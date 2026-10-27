package com.tailfriends.auth.service;

import com.tailfriends.auth.repository.RefreshTokenRepository;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 만료된 리프레시 토큰을 매일 04:30(KST)에 지운다.
 * 재발급에 쓴 토큰은 재사용 감지에 필요해서 만료 전에는 남긴다(TokenService.reissue).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenCleaner {

    private final RefreshTokenRepository refreshTokenRepository;
    private final Clock clock;

    @Scheduled(cron = "0 30 4 * * *", zone = "Asia/Seoul")
    @Transactional
    public void deleteExpired() {
        int deleted = refreshTokenRepository.deleteExpired(clock.instant());
        log.info("만료된 리프레시 토큰을 지웠어요: {}개", deleted);
    }
}
