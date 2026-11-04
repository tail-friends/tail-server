package com.tailfriends.photo.service;

import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.security.CurrentUser;
import com.tailfriends.photo.ai.EnclosureAnalysis;
import com.tailfriends.photo.ai.EnclosureAnalyzer;
import com.tailfriends.photo.domain.AiKind;
import com.tailfriends.photo.domain.Photo;
import com.tailfriends.photo.domain.PhotoPurpose;
import com.tailfriends.photo.dto.EnclosureAnalysisResponse;
import com.tailfriends.photo.repository.PhotoRepository;
import com.tailfriends.photo.storage.PhotoStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EnclosureAnalysisService {

    private final CurrentUser currentUser;
    private final PhotoRepository photoRepository;
    private final PhotoStorage photoStorage;
    private final EnclosureAnalyzer enclosureAnalyzer;
    private final AiUsageLimiter aiUsageLimiter;

    /**
     * 사육장 사진을 AI로 분석해 바닥·벽 색과 장식을 돌려준다. 저장하지 않는다.
     * 캐릭터 분석처럼 트랜잭션 없이 실행하고, 하루 횟수는 AI를 부르기 전에 잡아 두고 AI가 답을 주지 못하면 푼다.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public EnclosureAnalysisResponse analyze(Long photoId) {
        Long userId = currentUser.id();
        Photo photo = photoRepository.findByIdAndUserId(photoId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PHOTO_NOT_FOUND));
        if (photo.getPurpose() != PhotoPurpose.ENCLOSURE) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "사육장 사진으로 올린 사진만 분석할 수 있어요");
        }
        String sourceKey = photo.analysisSourceKey();
        if (sourceKey == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "분석할 사진이 없어요. 사육장 사진을 다시 올려 주세요");
        }
        Long usageId = aiUsageLimiter.reserve(userId, AiKind.ENCLOSURE);

        EnclosureAnalysis analysis;
        try {
            analysis = enclosureAnalyzer.analyze(photoStorage.read(sourceKey));
        } catch (RuntimeException e) {
            aiUsageLimiter.release(usageId);
            throw e;
        }

        if (!analysis.enclosure()) {
            throw new BusinessException(ErrorCode.NO_ENCLOSURE_IN_PHOTO);
        }
        return EnclosureAnalysisResponse.from(analysis);
    }
}
