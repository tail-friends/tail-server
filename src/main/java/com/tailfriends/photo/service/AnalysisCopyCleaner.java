package com.tailfriends.photo.service;

import com.tailfriends.photo.domain.Photo;
import com.tailfriends.photo.repository.PhotoRepository;
import com.tailfriends.photo.storage.PhotoStorage;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 원본 보관에 동의하지 않은 사용자의 분석용 사본은 올린 지 하루가 지나면 지운다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnalysisCopyCleaner {

    static final Duration KEEP_FOR = Duration.ofDays(1);

    private final PhotoRepository photoRepository;
    private final PhotoStorage photoStorage;
    private final Clock clock;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void deleteExpired() {
        List<Photo> expired = photoRepository.findAllByAnalysisKeyNotNullAndCreatedAtBefore(
                clock.instant().minus(KEEP_FOR));
        for (Photo photo : expired) {
            try {
                photoStorage.delete(photo.getAnalysisKey());
                photo.clearAnalysisKey();
            } catch (RuntimeException e) {
                log.warn("분석용 사본을 지우지 못했어요. 다음에 다시 시도해요: {}", photo.getAnalysisKey(), e);
            }
        }
    }
}
