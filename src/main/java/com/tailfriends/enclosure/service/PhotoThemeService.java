package com.tailfriends.enclosure.service;

import com.tailfriends.animal.domain.Animal;
import com.tailfriends.animal.repository.AnimalRepository;
import com.tailfriends.enclosure.domain.BackgroundType;
import com.tailfriends.enclosure.domain.Enclosure;
import com.tailfriends.enclosure.domain.EnclosureObject;
import com.tailfriends.enclosure.dto.BackgroundObjectRequest;
import com.tailfriends.enclosure.dto.EnclosureCreateRequest;
import com.tailfriends.enclosure.dto.EnclosureCreateResponse;
import com.tailfriends.enclosure.repository.EnclosureObjectRepository;
import com.tailfriends.enclosure.repository.EnclosureRepository;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.security.CurrentUser;
import com.tailfriends.photo.domain.Photo;
import com.tailfriends.photo.domain.PhotoPurpose;
import com.tailfriends.photo.repository.PhotoRepository;
import com.tailfriends.user.domain.User;
import com.tailfriends.user.repository.UserRepository;
import java.math.RoundingMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사진으로 만든 사육장 테마. 기능명세서 F-27. 사육장 분석(AI) 결과를 사용자가 확인한 뒤 저장한다.
 */
@Service
@RequiredArgsConstructor
public class PhotoThemeService {

    static final int MAX_PHOTO_THEMES = 10;

    private final CurrentUser currentUser;
    private final UserRepository userRepository;
    private final PhotoRepository photoRepository;
    private final AnimalRepository animalRepository;
    private final EnclosureRepository enclosureRepository;
    private final EnclosureObjectRepository objectRepository;

    /**
     * 사육장용(ENCLOSURE)으로 올린 내 사진으로만 만든다. setAsHomeFor를 보내면 그 개체의 평소 사육장으로도 정한다.
     * 사용자 행을 잠가서, 동시에 보내도 사진 테마가 10개를 넘지 않는다.
     */
    @Transactional
    public EnclosureCreateResponse create(EnclosureCreateRequest request) {
        Long userId = currentUser.id();
        Photo photo = photoRepository.findByIdAndUserId(request.photoId(), userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PHOTO_NOT_FOUND));
        if (photo.getPurpose() != PhotoPurpose.ENCLOSURE) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "사육장 사진으로 올린 사진만 테마로 만들 수 있어요");
        }
        Animal animal = request.setAsHomeFor() == null
                ? null
                : animalRepository.findByIdAndUserId(request.setAsHomeFor(), userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.ANIMAL_NOT_FOUND));
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        if (enclosureRepository.countByUserIdAndBackgroundType(userId, BackgroundType.PHOTO) >= MAX_PHOTO_THEMES) {
            throw new BusinessException(ErrorCode.ENCLOSURE_LIMIT_EXCEEDED);
        }

        Enclosure enclosure = enclosureRepository.save(
                Enclosure.photoTheme(user, photo.getId(), request.floorColor(), request.wallColor()));
        objectRepository.saveAll(request.objectsOrEmpty().stream()
                .map(object -> toEntity(enclosure, object))
                .toList());
        if (animal != null) {
            animal.changeHomeToPhotoTheme(enclosure.getId());
        }
        return new EnclosureCreateResponse(enclosure.getId());
    }

    /**
     * 사진 테마만 지운다. 배치한 장식과 사진에서 읽은 장식은 함께 지워지고(DB ON DELETE CASCADE),
     * 이 테마를 평소 사육장으로 쓰던 개체는 enclosure_id가 null이 되어(DB ON DELETE SET NULL) 종의 기본 테마로 돌아간다.
     * 사진과 산 아이템은 남는다.
     */
    @Transactional
    public void delete(Long enclosureId) {
        Enclosure enclosure = enclosureRepository.findByIdAndUserIdForUpdate(enclosureId, currentUser.id())
                .orElseThrow(() -> new BusinessException(ErrorCode.ENCLOSURE_NOT_FOUND));
        if (enclosure.isBuiltIn()) {
            throw new BusinessException(ErrorCode.BUILT_IN_THEME);
        }
        enclosureRepository.delete(enclosure);
    }

    private static EnclosureObject toEntity(Enclosure enclosure, BackgroundObjectRequest object) {
        return EnclosureObject.builder()
                .enclosure(enclosure)
                .type(object.type())
                .x(object.x().setScale(3, RoundingMode.HALF_UP))
                .y(object.y().setScale(3, RoundingMode.HALF_UP))
                .build();
    }
}
