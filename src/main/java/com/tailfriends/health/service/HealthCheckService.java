package com.tailfriends.health.service;

import com.tailfriends.animal.repository.AnimalRepository;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.security.CurrentUser;
import com.tailfriends.health.domain.HealthCheck;
import com.tailfriends.health.domain.HealthCheckKey;
import com.tailfriends.health.dto.HealthCheckListResponse;
import com.tailfriends.health.dto.HealthCheckResponse;
import com.tailfriends.health.dto.HealthCheckSummaryResponse;
import com.tailfriends.health.repository.HealthCheckRepository;
import com.tailfriends.photo.ai.HealthCheckAnalysis;
import com.tailfriends.photo.ai.HealthCheckAnalyzer;
import com.tailfriends.photo.domain.AiKind;
import com.tailfriends.photo.domain.Photo;
import com.tailfriends.photo.domain.PhotoPurpose;
import com.tailfriends.photo.repository.PhotoRepository;
import com.tailfriends.photo.service.AiUsageLimiter;
import com.tailfriends.photo.storage.PhotoStorage;
import com.tailfriends.species.domain.Shape;
import com.tailfriends.species.domain.Species;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 사진 건강 체크 (F-33, 기능명세서 4-10).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthCheckService {

    private final CurrentUser currentUser;
    private final PhotoRepository photoRepository;
    private final AnimalRepository animalRepository;
    private final HealthCheckRepository healthCheckRepository;
    private final HealthAlertService healthAlertService;
    private final HealthCheckAnalyzer healthCheckAnalyzer;
    private final AiUsageLimiter aiUsageLimiter;
    private final PhotoStorage photoStorage;
    private final TransactionTemplate transactionTemplate;

    /**
     * 건강 체크용(HEALTH)으로 올린 사진을 AI로 항목별 판정하고 저장한다. 꼬리(TAIL)는 게코만 보고,
     * 같은 개체의 직전 체크 사진과 비교한다. "확인 필요"가 있으면 HEALTH_CHECK_ATTENTION 경고를 연다.
     * AI 호출 동안 DB 연결을 잡지 않도록 읽기·AI 호출·저장을 나눠 실행한다. 하루 AI 횟수는 캐릭터 분석과 함께 센다.
     *
     * @throws BusinessException 없거나 남의 사진이면 PHOTO_NOT_FOUND, 건강 체크용 사진이 아니면 INVALID_INPUT,
     *                           파충류가 없으면 NO_REPTILE_IN_PHOTO, 횟수를 다 썼으면 AI_LIMIT_EXCEEDED,
     *                           AI를 쓸 수 없으면 AI_UNAVAILABLE
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public HealthCheckResponse check(Long photoId) {
        Long userId = currentUser.id();
        Target target = transactionTemplate.execute(status -> findTarget(userId, photoId));
        Long usageId = aiUsageLimiter.reserve(userId, AiKind.HEALTH_CHECK);

        HealthCheckAnalysis analysis;
        try {
            analysis = healthCheckAnalyzer.analyze(
                    photoStorage.read(target.sourceKey()), readQuietly(target.previousKey()), target.keys());
        } catch (RuntimeException e) {
            aiUsageLimiter.release(usageId);
            throw e;
        }
        if (!analysis.reptile()) {
            throw new BusinessException(ErrorCode.NO_REPTILE_IN_PHOTO);
        }

        return transactionTemplate.execute(status -> {
            HealthCheck check = healthCheckRepository.save(HealthCheck.of(
                    animalRepository.getReferenceById(target.animalId()),
                    photoRepository.getReferenceById(photoId),
                    analysis.items()));
            if (check.attentionCount() > 0) {
                healthAlertService.openForHealthCheck(check.getAnimal());
            }
            return HealthCheckResponse.from(check);
        });
    }

    /**
     * 최신순. cursor는 이전 페이지의 마지막 checkId다.
     *
     * @throws BusinessException 없거나 남의 개체면 ANIMAL_NOT_FOUND
     */
    @Transactional(readOnly = true)
    public HealthCheckListResponse getChecks(Long animalId, Long cursor, int size) {
        animalRepository.findByIdAndUserId(animalId, currentUser.id())
                .orElseThrow(() -> new BusinessException(ErrorCode.ANIMAL_NOT_FOUND));
        long before = cursor == null ? Long.MAX_VALUE : cursor;
        List<HealthCheck> checks = healthCheckRepository.findPage(animalId, before, PageRequest.of(0, size + 1));

        boolean hasNext = checks.size() > size;
        List<HealthCheck> page = hasNext ? checks.subList(0, size) : checks;
        Long nextCursor = hasNext ? page.getLast().getId() : null;
        return new HealthCheckListResponse(page.stream().map(this::toSummary).toList(), nextCursor);
    }

    private Target findTarget(Long userId, Long photoId) {
        Photo photo = photoRepository.findByIdAndUserId(photoId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PHOTO_NOT_FOUND));
        if (photo.getPurpose() != PhotoPurpose.HEALTH) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "건강 체크용으로 올린 사진만 체크할 수 있어요");
        }
        String sourceKey = photo.analysisSourceKey();
        if (sourceKey == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "체크할 사진이 없어요. 건강 체크용 사진을 다시 올려 주세요");
        }
        Long animalId = photo.getAnimal().getId();
        String previousKey = healthCheckRepository.findFirstByAnimalIdAndPhotoIsNotNullOrderByIdDesc(animalId)
                .map(HealthCheck::getPhoto)
                .filter(previous -> !previous.getId().equals(photoId))
                .map(previous -> previous.analysisSourceKey() != null
                        ? previous.analysisSourceKey() : previous.getThumbnailKey())
                .orElse(null);
        return new Target(animalId, sourceKey, previousKey, keysFor(photo.getAnimal().getSpecies()));
    }

    /**
     * 꼬리 두께로 영양 상태를 보는 건 게코뿐이라 다른 체형은 TAIL을 뺀다. 목록에 없는 종이면 체형을 몰라 뺀다.
     */
    static Set<HealthCheckKey> keysFor(Species species) {
        Set<HealthCheckKey> keys = EnumSet.allOf(HealthCheckKey.class);
        if (species == null || species.getShape() != Shape.GECKO) {
            keys.remove(HealthCheckKey.TAIL);
        }
        return keys;
    }

    /**
     * 비교용 사진은 없어도 체크할 수 있으니 읽지 못하면 null.
     */
    private byte[] readQuietly(String key) {
        if (key == null) {
            return null;
        }
        try {
            return photoStorage.read(key);
        } catch (RuntimeException e) {
            log.warn("이전 건강 체크 사진을 읽지 못했어요: {}", key, e);
            return null;
        }
    }

    private HealthCheckSummaryResponse toSummary(HealthCheck check) {
        Photo photo = check.getPhoto();
        return new HealthCheckSummaryResponse(
                check.getId(),
                photo == null ? null : photo.getId(),
                photo == null ? null : photoStorage.urlOf(photo.getThumbnailKey()),
                check.getCreatedAt(),
                check.getItems());
    }

    private record Target(Long animalId, String sourceKey, String previousKey, Set<HealthCheckKey> keys) {
    }
}
