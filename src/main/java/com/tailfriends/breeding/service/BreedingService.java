package com.tailfriends.breeding.service;

import com.tailfriends.animal.domain.Animal;
import com.tailfriends.animal.repository.AnimalRepository;
import com.tailfriends.breeding.domain.Breeding;
import com.tailfriends.breeding.dto.BreedingCreateRequest;
import com.tailfriends.breeding.dto.BreedingCreateResponse;
import com.tailfriends.breeding.repository.BreedingRepository;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.security.CurrentUser;
import com.tailfriends.global.util.KstDate;
import com.tailfriends.user.repository.UserRepository;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 번식 기록. 부모가 외부 개체면 이름만 저장하고, 내 개체여도 이름을 함께 저장해 개체를 지워도 남긴다.
 */
@Service
@RequiredArgsConstructor
public class BreedingService {

    private final CurrentUser currentUser;
    private final BreedingRepository breedingRepository;
    private final AnimalRepository animalRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    /**
     * 상태는 PAIRED로 시작한다.
     *
     * @throws BusinessException 암수 정보가 없거나 같은 개체를 암수로 보내거나 메이팅일이 오늘 이후면 INVALID_INPUT,
     *                           maleId·femaleId가 내 개체가 아니면 ANIMAL_NOT_FOUND
     */
    @Transactional
    public BreedingCreateResponse create(BreedingCreateRequest request) {
        Long userId = currentUser.id();
        if (request.pairedDate().isAfter(KstDate.today(clock))) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "메이팅일은 오늘 이후로 적을 수 없어요");
        }
        if (request.maleId() != null && request.maleId().equals(request.femaleId())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "같은 개체를 암수로 고를 수 없어요");
        }
        Parent male = parentOf(userId, request.maleId(), request.maleName(), "수컷");
        Parent female = parentOf(userId, request.femaleId(), request.femaleName(), "암컷");
        Breeding breeding = breedingRepository.save(Breeding.builder()
                .user(userRepository.getReferenceById(userId))
                .male(male.animal())
                .maleName(male.name())
                .female(female.animal())
                .femaleName(female.name())
                .pairedDate(request.pairedDate())
                .memo(request.memo())
                .build());
        return new BreedingCreateResponse(breeding.getId());
    }

    /**
     * ID가 있으면 내 개체와 그 이름, 없으면 외부 개체 이름.
     */
    private Parent parentOf(Long userId, Long animalId, String name, String label) {
        if (animalId != null) {
            Animal animal = animalRepository.findByIdAndUserId(animalId, userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.ANIMAL_NOT_FOUND));
            return new Parent(animal, animal.getName());
        }
        if (StringUtils.hasText(name)) {
            return new Parent(null, name.strip());
        }
        throw new BusinessException(ErrorCode.INVALID_INPUT, label + " 개체나 이름이 필요해요");
    }

    private record Parent(Animal animal, String name) {
    }
}
