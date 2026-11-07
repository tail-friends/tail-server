package com.tailfriends.photo.service;

import com.tailfriends.animal.repository.AnimalRepository;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.security.CurrentUser;
import com.tailfriends.health.repository.HealthCheckRepository;
import com.tailfriends.photo.domain.Photo;
import com.tailfriends.photo.domain.PhotoPurpose;
import com.tailfriends.photo.dto.AlbumPhotoResponse;
import com.tailfriends.photo.dto.AlbumResponse;
import com.tailfriends.photo.dto.HealthCheckSummary;
import com.tailfriends.photo.repository.PhotoRepository;
import com.tailfriends.photo.storage.PhotoStorage;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 성장 앨범. 기능명세서 F-22.
 */
@Service
@RequiredArgsConstructor
public class AlbumService {

    /**
     * 개체를 찍은 사진만 보여준다. 사육장(ENCLOSURE) 사진은 뺀다.
     */
    static final Set<PhotoPurpose> ALBUM_PURPOSES =
            EnumSet.of(PhotoPurpose.CHARACTER, PhotoPurpose.HEALTH, PhotoPurpose.ALBUM);

    private final CurrentUser currentUser;
    private final AnimalRepository animalRepository;
    private final PhotoRepository photoRepository;
    private final PhotoStorage photoStorage;
    private final HealthCheckRepository healthCheckRepository;

    /**
     * 최신순. cursor는 이전 페이지의 마지막 photoId다.
     *
     * @throws BusinessException 없거나 남의 개체면 ANIMAL_NOT_FOUND
     */
    @Transactional(readOnly = true)
    public AlbumResponse getAlbum(Long animalId, Long cursor, int size) {
        Long userId = currentUser.id();
        animalRepository.findByIdAndUserId(animalId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANIMAL_NOT_FOUND));
        long before = cursor == null ? Long.MAX_VALUE : cursor;
        List<Photo> photos = photoRepository.findAlbumPage(animalId, ALBUM_PURPOSES, before,
                PageRequest.of(0, size + 1));

        boolean hasNext = photos.size() > size;
        List<Photo> page = hasNext ? photos.subList(0, size) : photos;
        Long nextCursor = hasNext ? page.getLast().getId() : null;
        Map<Long, HealthCheckSummary> checks = latestChecksOf(page);
        return new AlbumResponse(page.stream()
                .map(photo -> AlbumPhotoResponse.of(photo, photoStorage.urlOf(photo.getThumbnailKey()),
                        checks.get(photo.getId())))
                .toList(), nextCursor);
    }

    /**
     * 사진 ID별로 가장 최근 건강 체크 요약. 쿼리 한 번으로 조회한다.
     */
    private Map<Long, HealthCheckSummary> latestChecksOf(List<Photo> photos) {
        if (photos.isEmpty()) {
            return Map.of();
        }
        return healthCheckRepository.findAllByPhotoIdIn(photos.stream().map(Photo::getId).toList()).stream()
                .collect(Collectors.toMap(
                        check -> check.getPhoto().getId(),
                        check -> check,
                        (a, b) -> a.getId() > b.getId() ? a : b))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> new HealthCheckSummary(e.getValue().getId(), e.getValue().attentionCount())));
    }
}
