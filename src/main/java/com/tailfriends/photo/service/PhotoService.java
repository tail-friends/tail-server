package com.tailfriends.photo.service;

import com.tailfriends.animal.domain.Animal;
import com.tailfriends.animal.repository.AnimalRepository;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.photo.domain.Photo;
import com.tailfriends.photo.domain.PhotoPurpose;
import com.tailfriends.photo.dto.PhotoUploadResponse;
import com.tailfriends.photo.repository.PhotoRepository;
import com.tailfriends.photo.storage.PhotoStorage;
import com.tailfriends.user.repository.UserRepository;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PhotoService {

    static final int THUMBNAIL_SIZE = 128;
    static final int ORIGINAL_SIZE = 1600;
    static final int ANALYSIS_SIZE = 1024;

    private final PhotoRepository photoRepository;
    private final AnimalRepository animalRepository;
    private final UserRepository userRepository;
    private final PhotoStorage photoStorage;
    private final ImageResizer imageResizer;

    /**
     * 128px 썸네일을 만들고, keepOriginal이면 긴 변 1600px로 줄인 원본도 보관한다. 모두 JPG로 저장한다.
     * 원본을 보관하지 않는 캐릭터 사진은 AI 분석용으로 긴 변 1024px 사본을 하루만 둔다.
     */
    @Transactional
    public PhotoUploadResponse upload(Long userId, Long animalId, PhotoPurpose purpose, boolean keepOriginal,
                                      MultipartFile file) {
        Animal animal = animalRepository.findByIdAndUserId(animalId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANIMAL_NOT_FOUND));
        boolean keepAnalysisCopy = !keepOriginal && purpose == PhotoPurpose.CHARACTER;
        byte[] bytes = read(file);
        List<byte[]> images = keepOriginal
                ? imageResizer.toJpegs(bytes, THUMBNAIL_SIZE, ORIGINAL_SIZE)
                : keepAnalysisCopy
                        ? imageResizer.toJpegs(bytes, THUMBNAIL_SIZE, ANALYSIS_SIZE)
                        : imageResizer.toJpegs(bytes, THUMBNAIL_SIZE);

        String name = "photos/" + UUID.randomUUID();
        String thumbnailKey = name + "_thumb.jpg";
        String originalKey = keepOriginal ? name + "_original.jpg" : null;
        String analysisKey = keepAnalysisCopy ? name + "_analysis.jpg" : null;
        String secondKey = originalKey != null ? originalKey : analysisKey;

        List<String> savedKeys = new ArrayList<>();
        try {
            photoStorage.save(thumbnailKey, images.get(0));
            savedKeys.add(thumbnailKey);
            if (secondKey != null) {
                photoStorage.save(secondKey, images.get(1));
                savedKeys.add(secondKey);
            }
            Photo photo = photoRepository.save(Photo.builder()
                    .user(userRepository.getReferenceById(userId))
                    .animal(animal)
                    .purpose(purpose)
                    .thumbnailKey(thumbnailKey)
                    .originalKey(originalKey)
                    .analysisKey(analysisKey)
                    .build());
            return new PhotoUploadResponse(photo.getId(), photoStorage.urlOf(thumbnailKey), originalKey != null);
        } catch (RuntimeException e) {
            savedKeys.forEach(this::deleteQuietly);
            throw e;
        }
    }

    private static byte[] read(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_FILE);
        }
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INVALID_FILE);
        }
    }

    private void deleteQuietly(String key) {
        try {
            photoStorage.delete(key);
        } catch (RuntimeException e) {
            log.warn("저장하다 실패한 사진 파일을 지우지 못했어요: {}", key, e);
        }
    }
}
