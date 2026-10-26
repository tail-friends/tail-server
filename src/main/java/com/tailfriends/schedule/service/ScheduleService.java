package com.tailfriends.schedule.service;

import com.tailfriends.animal.domain.Animal;
import com.tailfriends.animal.repository.AnimalRepository;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.security.CurrentUser;
import com.tailfriends.global.util.KstDate;
import com.tailfriends.schedule.domain.Schedule;
import com.tailfriends.schedule.dto.ScheduleCompleteRequest;
import com.tailfriends.schedule.dto.ScheduleCompleteResponse;
import com.tailfriends.schedule.dto.ScheduleCreateRequest;
import com.tailfriends.schedule.dto.ScheduleCreateResponse;
import com.tailfriends.schedule.dto.ScheduleListResponse;
import com.tailfriends.schedule.dto.ScheduleResponse;
import com.tailfriends.schedule.dto.ScheduleUpdateRequest;
import com.tailfriends.schedule.repository.ScheduleRepository;
import com.tailfriends.user.repository.UserRepository;
import java.time.Clock;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final CurrentUser currentUser;
    private final ScheduleRepository scheduleRepository;
    private final AnimalRepository animalRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    /**
     * animalId가 있으면 그 개체의 일정과 공통 일정, 없으면 내 일정 전부.
     */
    @Transactional(readOnly = true)
    public ScheduleListResponse getSchedules(Long animalId) {
        Long userId = currentUser.id();
        List<Schedule> schedules;
        if (animalId == null) {
            schedules = scheduleRepository.findAllOfUser(userId);
        } else {
            findAnimal(userId, animalId);
            schedules = scheduleRepository.findAllOfAnimal(userId, animalId);
        }
        return new ScheduleListResponse(schedules.stream().map(ScheduleResponse::from).toList());
    }

    @Transactional
    public ScheduleCreateResponse create(ScheduleCreateRequest request) {
        Long userId = currentUser.id();
        Animal animal = request.animalId() == null ? null : findAnimal(userId, request.animalId());
        Schedule schedule = scheduleRepository.save(Schedule.create(
                userRepository.getReferenceById(userId),
                animal,
                request.name().strip(),
                request.kind(),
                request.repeat().toRepeat(),
                request.startDate(),
                request.endDate()));
        return new ScheduleCreateResponse(schedule.getId(), schedule.getNextDate());
    }

    @Transactional
    public ScheduleResponse update(Long scheduleId, ScheduleUpdateRequest request) {
        Schedule schedule = findSchedule(currentUser.id(), scheduleId);
        schedule.update(
                request.name() == null ? null : request.name().strip(),
                request.kind(),
                request.repeat() == null ? null : request.repeat().toRepeat(),
                request.startDate(),
                request.endDate());
        return ScheduleResponse.from(schedule);
    }

    @Transactional
    public void delete(Long scheduleId) {
        scheduleRepository.delete(findSchedule(currentUser.id(), scheduleId));
    }

    /**
     * @throws BusinessException 완료일이 오늘(KST) 이후면 INVALID_INPUT
     */
    @Transactional
    public ScheduleCompleteResponse complete(Long scheduleId, ScheduleCompleteRequest request) {
        Schedule schedule = findSchedule(currentUser.id(), scheduleId);
        if (request.date().isAfter(KstDate.today(clock))) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "오늘 이후 날짜로 완료할 수 없어요");
        }
        schedule.complete(request.date());
        return new ScheduleCompleteResponse(schedule.getLastDoneDate(), schedule.getNextDate());
    }

    private Animal findAnimal(Long userId, Long animalId) {
        return animalRepository.findByIdAndUserId(animalId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANIMAL_NOT_FOUND));
    }

    private Schedule findSchedule(Long userId, Long scheduleId) {
        return scheduleRepository.findByIdAndUserId(scheduleId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));
    }
}
