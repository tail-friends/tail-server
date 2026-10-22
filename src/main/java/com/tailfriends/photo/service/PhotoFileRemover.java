package com.tailfriends.photo.service;

import com.tailfriends.photo.repository.PhotoFileKeys;
import com.tailfriends.photo.repository.PhotoRepository;
import com.tailfriends.photo.storage.PhotoStorage;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 개체 삭제·회원 탈퇴 때 사진 파일을 지운다. 사진 행은 DB의 ON DELETE CASCADE로 지워지지만 파일은 남기 때문이다.
 *
 * <p>호출한 트랜잭션이 커밋된 뒤에 지운다. 롤백되면 파일은 그대로 남는다. 지우다 실패한 파일은 로그만 남긴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PhotoFileRemover {

    private final PhotoRepository photoRepository;
    private final PhotoStorage photoStorage;

    public void removeFilesOfUserAfterCommit(Long userId) {
        removeAfterCommit(keysOf(photoRepository.findFileKeysByUserId(userId)));
    }

    public void removeFilesOfAnimalAfterCommit(Long animalId) {
        removeAfterCommit(keysOf(photoRepository.findFileKeysByAnimalId(animalId)));
    }

    /**
     * 이 파일들을 호출한 트랜잭션이 커밋된 뒤에 지운다.
     */
    public void removeAfterCommit(List<String> keys) {
        if (keys.isEmpty()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                keys.forEach(PhotoFileRemover.this::deleteQuietly);
            }
        });
    }

    private static List<String> keysOf(List<PhotoFileKeys> photos) {
        return photos.stream().flatMap(photo -> photo.all().stream()).toList();
    }

    private void deleteQuietly(String key) {
        try {
            photoStorage.delete(key);
        } catch (RuntimeException e) {
            log.warn("사진 파일을 지우지 못했어요: {}", key, e);
        }
    }
}
