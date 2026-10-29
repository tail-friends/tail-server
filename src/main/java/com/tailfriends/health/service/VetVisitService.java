package com.tailfriends.health.service;

import com.tailfriends.animal.domain.Animal;
import com.tailfriends.animal.repository.AnimalRepository;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.security.CurrentUser;
import com.tailfriends.global.util.KstDate;
import com.tailfriends.health.domain.VetVisit;
import com.tailfriends.health.dto.VetVisitCreateRequest;
import com.tailfriends.health.dto.VetVisitCreateRequest.MedicationSchedule;
import com.tailfriends.health.dto.VetVisitCreateResponse;
import com.tailfriends.health.dto.VetVisitListResponse;
import com.tailfriends.health.dto.VetVisitResponse;
import com.tailfriends.health.dto.VetVisitUpdateRequest;
import com.tailfriends.health.repository.VetVisitRepository;
import com.tailfriends.schedule.domain.ScheduleKind;
import com.tailfriends.schedule.dto.ScheduleCreateRequest;
import com.tailfriends.schedule.service.ScheduleService;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 진료 기록. 다음 방문일 알림은 지금 서버가 보내지 않는다(서버 푸시가 생기면 추가, 그전엔 앱 로컬 알림).
 */
@Service
@RequiredArgsConstructor
public class VetVisitService {

    private final CurrentUser currentUser;
    private final VetVisitRepository vetVisitRepository;
    private final AnimalRepository animalRepository;
    private final ScheduleService scheduleService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public VetVisitListResponse getVisits(Long animalId) {
        Long userId = currentUser.id();
        findAnimal(userId, animalId);
        return new VetVisitListResponse(vetVisitRepository.findAllByAnimalIdOrderByVisitDateDescIdDesc(animalId)
                .stream().map(VetVisitResponse::from).toList());
    }

    /**
     * createMedicationSchedule이 있으면 같은 트랜잭션에서 투약 일정(MEDICATION, 오늘부터)도 만든다.
     * 일정을 만들지 못하면 진료 기록도 저장하지 않는다.
     *
     * @throws BusinessException 날짜 규칙을 어기면 INVALID_INPUT, 내 개체가 아니면 ANIMAL_NOT_FOUND
     */
    @Transactional
    public VetVisitCreateResponse create(VetVisitCreateRequest request) {
        Long userId = currentUser.id();
        Animal animal = findAnimal(userId, request.animalId());
        validateDates(request.visitDate(), request.nextVisitDate());
        VetVisit visit = vetVisitRepository.save(VetVisit.builder()
                .animal(animal)
                .hospital(strip(request.hospital()))
                .visitDate(request.visitDate())
                .symptom(request.symptom())
                .diagnosis(request.diagnosis())
                .prescription(request.prescription())
                .nextVisitDate(request.nextVisitDate())
                .build());
        Long scheduleId = request.createMedicationSchedule() == null
                ? null
                : createMedicationSchedule(animal, request.createMedicationSchedule());
        return new VetVisitCreateResponse(visit.getId(), scheduleId);
    }

    /**
     * @throws BusinessException 바꾼 뒤 날짜 규칙을 어기면 INVALID_INPUT, 없거나 남의 기록이면 VISIT_NOT_FOUND
     */
    @Transactional
    public VetVisitResponse update(Long visitId, VetVisitUpdateRequest request) {
        VetVisit visit = findVisit(currentUser.id(), visitId);
        LocalDate visitDate = request.visitDate() != null ? request.visitDate() : visit.getVisitDate();
        LocalDate nextVisitDate = request.nextVisitDate() != null ? request.nextVisitDate() : visit.getNextVisitDate();
        validateDates(visitDate, nextVisitDate);
        visit.update(strip(request.hospital()), request.visitDate(), request.symptom(), request.diagnosis(),
                request.prescription(), request.nextVisitDate());
        return VetVisitResponse.from(visit);
    }

    /**
     * 함께 만든 투약 일정은 남긴다.
     */
    @Transactional
    public void delete(Long visitId) {
        vetVisitRepository.delete(findVisit(currentUser.id(), visitId));
    }

    private Long createMedicationSchedule(Animal animal, MedicationSchedule medication) {
        return scheduleService.create(new ScheduleCreateRequest(
                animal.getId(),
                medication.name(),
                ScheduleKind.MEDICATION,
                medication.repeat(),
                KstDate.today(clock),
                medication.endDate())).id();
    }

    private void validateDates(LocalDate visitDate, LocalDate nextVisitDate) {
        if (visitDate.isAfter(KstDate.today(clock))) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "방문일은 오늘 이후로 적을 수 없어요");
        }
        if (nextVisitDate != null && !nextVisitDate.isAfter(visitDate)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "다음 방문일은 방문일보다 늦어야 해요");
        }
    }

    private Animal findAnimal(Long userId, Long animalId) {
        return animalRepository.findByIdAndUserId(animalId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANIMAL_NOT_FOUND));
    }

    private VetVisit findVisit(Long userId, Long visitId) {
        return vetVisitRepository.findByIdAndUserId(visitId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VISIT_NOT_FOUND));
    }

    private static String strip(String value) {
        return value == null ? null : value.strip();
    }
}
