package com.tailfriends.breeding.service;

import com.tailfriends.animal.domain.Animal;
import com.tailfriends.animal.repository.AnimalRepository;
import com.tailfriends.breeding.domain.Breeding;
import com.tailfriends.breeding.domain.Clutch;
import com.tailfriends.breeding.dto.BreedingCreateRequest;
import com.tailfriends.breeding.dto.BreedingCreateResponse;
import com.tailfriends.breeding.dto.BreedingListResponse;
import com.tailfriends.breeding.dto.BreedingResponse;
import com.tailfriends.breeding.dto.BreedingUpdateRequest;
import com.tailfriends.breeding.dto.ClutchRequest;
import com.tailfriends.breeding.dto.ClutchResponse;
import com.tailfriends.breeding.repository.BreedingRepository;
import com.tailfriends.breeding.repository.ClutchRepository;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.security.CurrentUser;
import com.tailfriends.global.util.KstDate;
import com.tailfriends.user.repository.UserRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
    private final ClutchRepository clutchRepository;
    private final AnimalRepository animalRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    /**
     * 메이팅일 최신순. 페이지를 나누지 않는다.
     */
    @Transactional(readOnly = true)
    public BreedingListResponse getBreedings() {
        return new BreedingListResponse(toResponses(breedingRepository.findAllWithParentsByUserId(currentUser.id())));
    }

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
     * clutches를 보내면 날짜를 모두 확인한 뒤 기존 산란 기록을 지우고 보낸 목록으로 바꾼다. 상태는 순서와 관계없이 바꾼다.
     *
     * @throws BusinessException 산란일이 메이팅일 전이거나 오늘 이후, 예상 해칭일이 산란일 전이면 INVALID_INPUT,
     *                           없거나 남의 번식 기록이면 BREEDING_NOT_FOUND
     */
    @Transactional
    public BreedingResponse update(Long breedingId, BreedingUpdateRequest request) {
        Breeding breeding = findBreeding(currentUser.id(), breedingId);
        if (request.clutches() != null) {
            request.clutches().forEach(clutch -> validateClutch(breeding, clutch));
            clutchRepository.deleteAllByBreedingId(breeding.getId());
            clutchRepository.saveAll(request.clutches().stream()
                    .map(clutch -> Clutch.builder()
                            .breeding(breeding)
                            .laidDate(clutch.laidDate())
                            .eggCount(clutch.eggCount())
                            .incubationTemp(clutch.incubationTemp())
                            .expectedHatchDate(clutch.expectedHatchDate())
                            .build())
                    .toList());
        }
        if (request.status() != null) {
            breeding.changeStatus(request.status());
        }
        return toResponses(List.of(breeding)).getFirst();
    }

    private void validateClutch(Breeding breeding, ClutchRequest clutch) {
        if (!isBetweenPairedDateAndToday(breeding, clutch.laidDate())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "산란일은 메이팅일부터 오늘까지만 적을 수 있어요");
        }
        if (clutch.expectedHatchDate() != null && clutch.expectedHatchDate().isBefore(clutch.laidDate())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "예상 해칭일은 산란일보다 빠를 수 없어요");
        }
    }

    private boolean isBetweenPairedDateAndToday(Breeding breeding, LocalDate date) {
        return !date.isBefore(breeding.getPairedDate()) && !date.isAfter(KstDate.today(clock));
    }

    private Breeding findBreeding(Long userId, Long breedingId) {
        return breedingRepository.findByIdAndUserId(breedingId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BREEDING_NOT_FOUND));
    }

    /**
     * 산란 기록과 해칭 개체를 번식 기록 수와 관계없이 쿼리 두 번으로 읽는다.
     */
    private List<BreedingResponse> toResponses(List<Breeding> breedings) {
        if (breedings.isEmpty()) {
            return List.of();
        }
        List<Long> ids = breedings.stream().map(Breeding::getId).toList();
        Map<Long, List<ClutchResponse>> clutches = clutchRepository.findAllByBreedingIdInOrderByLaidDateAscIdAsc(ids)
                .stream()
                .collect(Collectors.groupingBy(clutch -> clutch.getBreeding().getId(),
                        Collectors.mapping(ClutchResponse::from, Collectors.toList())));
        Map<Long, List<Long>> hatchlings = animalRepository.findAllByBreedingIdInOrderByIdAsc(ids).stream()
                .collect(Collectors.groupingBy(Animal::getBreedingId,
                        Collectors.mapping(Animal::getId, Collectors.toList())));
        return breedings.stream()
                .map(breeding -> BreedingResponse.of(breeding,
                        clutches.getOrDefault(breeding.getId(), List.of()),
                        hatchlings.getOrDefault(breeding.getId(), List.of())))
                .toList();
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
