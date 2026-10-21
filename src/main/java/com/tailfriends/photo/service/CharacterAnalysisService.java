package com.tailfriends.photo.service;

import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.util.KstDate;
import com.tailfriends.photo.ai.AiProperties;
import com.tailfriends.photo.ai.CharacterAnalysis;
import com.tailfriends.photo.ai.CharacterAnalyzer;
import com.tailfriends.photo.domain.AiKind;
import com.tailfriends.photo.domain.AiUsage;
import com.tailfriends.photo.domain.Photo;
import com.tailfriends.photo.dto.CharacterAnalysisResponse;
import com.tailfriends.photo.repository.AiUsageRepository;
import com.tailfriends.photo.repository.PhotoRepository;
import com.tailfriends.photo.storage.PhotoStorage;
import com.tailfriends.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CharacterAnalysisService {

    private final PhotoRepository photoRepository;
    private final AiUsageRepository aiUsageRepository;
    private final UserRepository userRepository;
    private final PhotoStorage photoStorage;
    private final CharacterAnalyzer characterAnalyzer;
    private final AiProperties aiProperties;
    private final Clock clock;

    /**
     * 사진을 AI로 분석해 캐릭터 값을 돌려준다. 저장하지 않는다.
     * AI 호출이 몇 초 걸려 그동안 DB 연결을 잡고 있지 않도록 트랜잭션 없이 실행한다.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public CharacterAnalysisResponse analyze(Long userId, Long photoId) {
        Photo photo = photoRepository.findByIdAndUserId(photoId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PHOTO_NOT_FOUND));
        String sourceKey = photo.analysisSourceKey();
        if (sourceKey == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "분석할 사진이 없어요. 캐릭터용 사진을 다시 올려 주세요");
        }
        if (usedToday(userId) >= aiProperties.dailyLimit()) {
            throw new BusinessException(ErrorCode.AI_LIMIT_EXCEEDED);
        }

        CharacterAnalysis analysis;
        try {
            analysis = characterAnalyzer.analyze(photoStorage.read(sourceKey));
        } catch (BusinessException e) {
            record(userId, false);
            throw e;
        }
        record(userId, true);

        if (!analysis.reptile()) {
            throw new BusinessException(ErrorCode.NO_REPTILE_IN_PHOTO);
        }
        return CharacterAnalysisResponse.from(analysis);
    }

    /**
     * 오늘(KST) AI가 답을 준 분석 수. 모든 종류의 AI 분석을 함께 센다.
     */
    private long usedToday(Long userId) {
        Instant startOfToday = KstDate.today(clock).atStartOfDay(KstDate.ZONE).toInstant();
        return aiUsageRepository.countByUserIdAndSuccessTrueAndCreatedAtGreaterThanEqual(userId, startOfToday);
    }

    private void record(Long userId, boolean success) {
        aiUsageRepository.save(AiUsage.of(userRepository.getReferenceById(userId), AiKind.CHARACTER, success));
    }
}
