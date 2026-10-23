package com.tailfriends.photo.service;

import com.tailfriends.global.security.CurrentUser;
import com.tailfriends.photo.domain.Photo;
import com.tailfriends.photo.dto.OriginalDeleteResponse;
import com.tailfriends.photo.repository.PhotoRepository;
import com.tailfriends.user.domain.ConsentType;
import com.tailfriends.user.service.ConsentService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OriginalPhotoService {

    private final CurrentUser currentUser;
    private final PhotoRepository photoRepository;
    private final PhotoFileRemover photoFileRemover;
    private final ConsentService consentService;

    /**
     * 보관 중인 원본 사진을 모두 지우고 원본 사진 보관 동의를 철회한다. 썸네일은 남는다.
     * 파일은 커밋 후 지운다. 사용자가 없으면(탈퇴 등) UNAUTHORIZED.
     */
    @Transactional
    public OriginalDeleteResponse deleteAll() {
        Long userId = currentUser.id();
        consentService.changeOptional(userId, ConsentType.KEEP_ORIGINAL, false);
        List<Photo> photos = photoRepository.findAllByUserIdAndOriginalKeyNotNull(userId);
        List<String> keys = photos.stream().map(Photo::getOriginalKey).toList();
        photos.forEach(Photo::clearOriginalKey);
        photoFileRemover.removeAfterCommit(keys);
        return new OriginalDeleteResponse(photos.size());
    }
}
