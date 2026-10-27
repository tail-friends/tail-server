package com.tailfriends.photo.service;

import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.security.CurrentUser;
import com.tailfriends.photo.ai.CharacterAnalysis;
import com.tailfriends.photo.ai.CharacterAnalyzer;
import com.tailfriends.photo.domain.AiKind;
import com.tailfriends.photo.domain.Photo;
import com.tailfriends.photo.dto.CharacterAnalysisResponse;
import com.tailfriends.photo.repository.PhotoRepository;
import com.tailfriends.photo.storage.PhotoStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CharacterAnalysisService {

    private final CurrentUser currentUser;
    private final PhotoRepository photoRepository;
    private final PhotoStorage photoStorage;
    private final CharacterAnalyzer characterAnalyzer;
    private final AiUsageLimiter aiUsageLimiter;

    /**
     * 사진을 AI로 분석해 캐릭터 값을 돌려준다. 저장하지 않는다.
     * AI 호출이 몇 초 걸려 그동안 DB 연결을 잡고 있지 않도록 트랜잭션 없이 실행한다.
     * 하루 횟수는 AI를 부르기 전에 잡아 두고, AI가 답을 주지 못하면 푼다.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public CharacterAnalysisResponse analyze(Long photoId) {
        Long userId = currentUser.id();
        Photo photo = photoRepository.findByIdAndUserId(photoId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PHOTO_NOT_FOUND));
        String sourceKey = photo.analysisSourceKey();
        if (sourceKey == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "분석할 사진이 없어요. 캐릭터용 사진을 다시 올려 주세요");
        }
        Long usageId = aiUsageLimiter.reserve(userId, AiKind.CHARACTER);

        CharacterAnalysis analysis;
        try {
            analysis = characterAnalyzer.analyze(photoStorage.read(sourceKey));
        } catch (RuntimeException e) {
            aiUsageLimiter.release(usageId);
            throw e;
        }

        if (!analysis.reptile()) {
            throw new BusinessException(ErrorCode.NO_REPTILE_IN_PHOTO);
        }
        return CharacterAnalysisResponse.from(analysis);
    }
}
