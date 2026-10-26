package com.tailfriends.today.service;

import com.tailfriends.animal.domain.Animal;
import com.tailfriends.animal.domain.FeedSchedule;
import com.tailfriends.animal.domain.FeedStatus;
import com.tailfriends.animal.repository.AnimalRepository;
import com.tailfriends.animal.service.FeedService;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.security.CurrentUser;
import com.tailfriends.global.util.KstDate;
import com.tailfriends.health.repository.HealthAlertRepository;
import com.tailfriends.log.repository.LogRepository;
import com.tailfriends.photo.repository.AnimalThumbnail;
import com.tailfriends.photo.repository.PhotoRepository;
import com.tailfriends.photo.storage.PhotoStorage;
import com.tailfriends.schedule.domain.Schedule;
import com.tailfriends.schedule.repository.ScheduleRepository;
import com.tailfriends.today.dto.TodayResponse;
import com.tailfriends.today.dto.TodayResponse.Suggested;
import com.tailfriends.today.dto.TodayResponse.TodayAlert;
import com.tailfriends.today.dto.TodayResponse.TodayDone;
import com.tailfriends.today.dto.TodayResponse.TodayFeed;
import com.tailfriends.today.dto.TodayResponse.TodaySchedule;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TodayService {

    private static final Set<FeedStatus> FEED_TODO = EnumSet.of(FeedStatus.DUE, FeedStatus.OVERDUE);

    private final CurrentUser currentUser;
    private final AnimalRepository animalRepository;
    private final FeedService feedService;
    private final LogRepository logRepository;
    private final PhotoRepository photoRepository;
    private final PhotoStorage photoStorage;
    private final ScheduleRepository scheduleRepository;
    private final HealthAlertRepository healthAlertRepository;
    private final Clock clock;

    /**
     * @param date 없으면 오늘(KST). 지금은 오늘만 된다
     * @throws BusinessException 오늘이 아닌 날짜면 INVALID_INPUT
     */
    @Transactional(readOnly = true)
    public TodayResponse getToday(LocalDate date) {
        Long userId = currentUser.id();
        LocalDate today = KstDate.today(clock);
        if (date != null && !date.equals(today)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "오늘 날짜만 조회할 수 있어요");
        }
        return new TodayResponse(today, feeds(userId), schedules(userId, today), alerts(userId), done(userId, today));
    }

    /**
     * 급여가 오늘이거나 지난 개체, 다음 급여일이 이른 순.
     */
    private List<TodayFeed> feeds(Long userId) {
        List<Animal> animals = animalRepository.findAllByUserIdOrderByIdAsc(userId);
        Map<Long, FeedSchedule> schedules = feedService.schedulesOf(animals);
        List<Animal> todo = animals.stream()
                .filter(animal -> FEED_TODO.contains(schedules.get(animal.getId()).status()))
                .sorted(Comparator.comparing((Animal animal) -> schedules.get(animal.getId()).nextDate())
                        .thenComparing(Animal::getId))
                .toList();
        if (todo.isEmpty()) {
            return List.of();
        }
        Map<Long, String> thumbnails = latestThumbnails(todo.stream().map(Animal::getId).toList());
        return todo.stream().map(animal -> {
            FeedSchedule feed = schedules.get(animal.getId());
            Integer overdueDays = feed.status() == FeedStatus.OVERDUE ? -feed.daysLeft() : null;
            return new TodayFeed(animal.getId(), animal.getName(), thumbnails.get(animal.getId()), feed.status(),
                    overdueDays, suggested(animal.getId()));
        }).toList();
    }

    private Map<Long, String> latestThumbnails(List<Long> animalIds) {
        Map<Long, String> thumbnails = new HashMap<>();
        for (AnimalThumbnail thumbnail : photoRepository.findCharacterThumbnails(animalIds)) {
            thumbnails.putIfAbsent(thumbnail.animalId(), photoStorage.urlOf(thumbnail.thumbnailKey()));
        }
        return thumbnails;
    }

    private Suggested suggested(Long animalId) {
        return logRepository.findLatestFedFoods(animalId, PageRequest.of(0, 1)).stream()
                .findFirst()
                .map(food -> new Suggested(food.food(), food.count()))
                .orElse(null);
    }

    private List<TodaySchedule> schedules(Long userId, LocalDate today) {
        return scheduleRepository.findDueOfUser(userId, today).stream()
                .map(schedule -> new TodaySchedule(schedule.getId(), animalIdOf(schedule), schedule.getName(),
                        schedule.getNextDate()))
                .toList();
    }

    private List<TodayAlert> alerts(Long userId) {
        return healthAlertRepository.findOpenByUser(userId).stream()
                .map(alert -> new TodayAlert(alert.id(), alert.animalId(), alert.type()))
                .toList();
    }

    /**
     * 오늘 남긴 기록(저장한 순) 다음에 오늘 완료한 일정.
     */
    private List<TodayDone> done(Long userId, LocalDate today) {
        Stream<TodayDone> logs = logRepository.findDoneLogs(userId, today).stream()
                .map(log -> new TodayDone(log.type().name(), log.animalId(), log.logId(), null));
        Stream<TodayDone> schedules = scheduleRepository.findDoneOfUser(userId, today).stream()
                .map(schedule -> new TodayDone(TodayDone.SCHEDULE, animalIdOf(schedule), null, schedule.getId()));
        return Stream.concat(logs, schedules).toList();
    }

    private static Long animalIdOf(Schedule schedule) {
        return schedule.getAnimal() == null ? null : schedule.getAnimal().getId();
    }
}
