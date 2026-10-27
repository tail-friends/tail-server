package com.tailfriends.animal.service;

import com.tailfriends.animal.domain.Animal;
import com.tailfriends.animal.domain.FeedSchedule;
import com.tailfriends.animal.dto.AnimalCreateRequest;
import com.tailfriends.animal.dto.AnimalCreateResponse;
import com.tailfriends.animal.dto.AnimalListResponse;
import com.tailfriends.animal.dto.AnimalResponse;
import com.tailfriends.animal.dto.AnimalSummaryResponse;
import com.tailfriends.animal.dto.AnimalUpdateRequest;
import com.tailfriends.animal.repository.AnimalRepository;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.security.CurrentUser;
import com.tailfriends.global.util.KstDate;
import com.tailfriends.health.dto.AlertResponse;
import com.tailfriends.health.service.HealthAlertService;
import com.tailfriends.photo.service.PhotoFileRemover;
import com.tailfriends.species.domain.Species;
import com.tailfriends.species.repository.SpeciesRepository;
import com.tailfriends.user.repository.UserRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AnimalService {

    private final CurrentUser currentUser;
    private final AnimalRepository animalRepository;
    private final SpeciesRepository speciesRepository;
    private final UserRepository userRepository;
    private final FeedService feedService;
    private final HealthAlertService healthAlertService;
    private final AnimalStatusService animalStatusService;
    private final Clock clock;
    private final PhotoFileRemover photoFileRemover;

    @Transactional
    public AnimalCreateResponse create(AnimalCreateRequest request) {
        Long userId = currentUser.id();
        validateHatchDate(request.hatchDate());
        Species species = null;
        String speciesName;
        if (request.speciesId() != null) {
            species = findSpecies(request.speciesId());
            speciesName = species.getName();
        } else if (StringUtils.hasText(request.speciesName())) {
            speciesName = request.speciesName().strip();
        } else {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "speciesId 또는 speciesName이 필요해요");
        }

        Animal animal = animalRepository.save(Animal.builder()
                .user(userRepository.getReferenceById(userId))
                .species(species)
                .speciesName(speciesName)
                .name(request.name().strip())
                .morph(request.morph())
                .sex(request.sex())
                .hatchDate(request.hatchDate())
                .feedIntervalDays(request.feedIntervalDays())
                .memo(request.memo())
                .build());
        return new AnimalCreateResponse(animal.getId());
    }

    @Transactional(readOnly = true)
    public AnimalListResponse getAnimals() {
        Long userId = currentUser.id();
        List<Animal> animals = animalRepository.findAllByUserIdOrderByIdAsc(userId);
        Map<Long, FeedSchedule> feeds = feedService.schedulesOf(animals);
        Map<Long, Long> alertCounts = healthAlertService.openCountsOf(animals);
        return new AnimalListResponse(animals.stream()
                .map(animal -> AnimalSummaryResponse.of(animal, feeds.get(animal.getId()),
                        alertCounts.getOrDefault(animal.getId(), 0L)))
                .toList());
    }

    @Transactional(readOnly = true)
    public AnimalResponse getAnimal(Long animalId) {
        Long userId = currentUser.id();
        return toResponse(findAnimal(userId, animalId));
    }

    @Transactional
    public AnimalResponse update(Long animalId, AnimalUpdateRequest request) {
        Long userId = currentUser.id();
        Animal animal = findAnimal(userId, animalId);
        validateHatchDate(request.hatchDate());
        if (request.speciesId() != null) {
            Species species = findSpecies(request.speciesId());
            animal.changeSpecies(species, species.getName());
        } else if (request.speciesName() != null) {
            animal.changeSpecies(null, request.speciesName().strip());
        }
        animal.update(
                request.name() == null ? null : request.name().strip(),
                request.morph(),
                request.sex(),
                request.hatchDate(),
                request.feedIntervalDays(),
                request.memo());
        return toResponse(animal);
    }

    /**
     * 기록·캐릭터·사진·건강 경고 행은 DB의 ON DELETE CASCADE로 함께 지워진다. 사진 파일은 커밋 후 지운다.
     */
    @Transactional
    public void delete(Long animalId) {
        Long userId = currentUser.id();
        Animal animal = findAnimal(userId, animalId);
        photoFileRemover.removeFilesOfAnimalAfterCommit(animal.getId());
        animalRepository.delete(animal);
    }

    private AnimalResponse toResponse(Animal animal) {
        FeedSchedule feed = feedService.scheduleOf(animal);
        List<AlertResponse> alerts = healthAlertService.openAlertsOf(animal);
        return AnimalResponse.of(animal, feed, alerts,
                animalStatusService.statusOf(animal, feed, !alerts.isEmpty()));
    }

    /**
     * 없거나 남의 개체면 존재를 숨기기 위해 똑같이 ANIMAL_NOT_FOUND.
     */
    private Animal findAnimal(Long userId, Long animalId) {
        return animalRepository.findByIdAndUserId(animalId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANIMAL_NOT_FOUND));
    }

    private Species findSpecies(Long speciesId) {
        return speciesRepository.findById(speciesId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT, "존재하지 않는 종이에요"));
    }

    private void validateHatchDate(LocalDate hatchDate) {
        if (hatchDate != null && hatchDate.isAfter(KstDate.today(clock))) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "해칭일은 오늘 이후일 수 없어요");
        }
    }
}
