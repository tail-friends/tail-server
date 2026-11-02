package com.tailfriends.photo.service;

import com.tailfriends.animal.repository.AnimalRepository;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.security.CurrentUser;
import com.tailfriends.photo.domain.Photo;
import com.tailfriends.photo.domain.PhotoPurpose;
import com.tailfriends.photo.dto.AlbumPhotoResponse;
import com.tailfriends.photo.dto.AlbumResponse;
import com.tailfriends.photo.repository.PhotoRepository;
import com.tailfriends.photo.storage.PhotoStorage;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
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
        return new AlbumResponse(page.stream()
                .map(photo -> AlbumPhotoResponse.of(photo, photoStorage.urlOf(photo.getThumbnailKey())))
                .toList(), nextCursor);
    }
}
