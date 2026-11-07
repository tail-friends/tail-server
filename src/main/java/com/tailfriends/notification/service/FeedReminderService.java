package com.tailfriends.notification.service;

import com.tailfriends.animal.domain.Animal;
import com.tailfriends.animal.domain.FeedSchedule;
import com.tailfriends.animal.domain.FeedStatus;
import com.tailfriends.animal.repository.AnimalRepository;
import com.tailfriends.animal.service.FeedService;
import com.tailfriends.global.util.KstDate;
import com.tailfriends.notification.domain.FeedReminder;
import com.tailfriends.notification.domain.PushToken;
import com.tailfriends.notification.fcm.PushSender;
import com.tailfriends.notification.repository.PushTokenRepository;
import java.time.Clock;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 급여일 서버 푸시 (F-08). 사용자 알림 시간(KST)에 오늘 급여일·급여일이 2일 지난 개체를 묶어 한 번 보낸다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedReminderService {

    static final int WINDOW_MINUTES = 10;

    private final PushTokenRepository pushTokenRepository;
    private final AnimalRepository animalRepository;
    private final FeedService feedService;
    private final PushSender pushSender;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    /**
     * 지금(KST)이 속한 10분 구간에 알림 시간이 있는 사용자에게 보낸다. 스케줄러가 10분마다 부른다.
     * 발송은 네트워크를 타서 트랜잭션 밖에서 하고, 쓸 수 없게 된 토큰은 지운다.
     *
     * @return 푸시를 보낸 사용자 수
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public int sendDueReminders() {
        LocalTime from = windowStart(ZonedDateTime.now(clock.withZone(KstDate.ZONE)).toLocalTime());
        LocalTime to = from.plusMinutes(WINDOW_MINUTES);
        // 23:50 구간은 다음 날 00:00까지
        List<Long> userIds = pushTokenRepository.findUserIdsToNotify(from, to.equals(LocalTime.MIDNIGHT)
                ? LocalTime.MAX : to);
        if (userIds.isEmpty()) {
            return 0;
        }
        Map<Long, List<String>> tokens = pushTokenRepository.findAllByUserIdIn(userIds).stream()
                .collect(Collectors.groupingBy(token -> token.getUser().getId(),
                        Collectors.mapping(PushToken::getToken, Collectors.toList())));

        int sent = 0;
        for (Long userId : userIds) {
            Optional<String> body = transactionTemplate.execute(status -> bodyFor(userId));
            List<String> userTokens = tokens.getOrDefault(userId, List.of());
            if (body.isEmpty() || userTokens.isEmpty()) {
                continue;
            }
            List<String> invalid = pushSender.send(userTokens, FeedReminder.TITLE, body.get());
            if (!invalid.isEmpty()) {
                transactionTemplate.executeWithoutResult(status -> pushTokenRepository.deleteByTokenIn(invalid));
            }
            sent++;
        }
        log.info("급여일 푸시 {}명 ({}~{})", sent, from, to);
        return sent;
    }

    static LocalTime windowStart(LocalTime now) {
        return LocalTime.of(now.getHour(), now.getMinute() / WINDOW_MINUTES * WINDOW_MINUTES);
    }

    /**
     * 오늘 급여일(DUE)과 급여일이 딱 2일 지난 개체. 쿨링 중이거나 기록이 없는 개체는 빠진다.
     */
    private Optional<String> bodyFor(Long userId) {
        List<Animal> animals = animalRepository.findAllByUserIdOrderByIdAsc(userId);
        Map<Long, FeedSchedule> schedules = feedService.schedulesOf(animals);
        List<String> dueToday = animals.stream()
                .filter(animal -> schedules.get(animal.getId()).status() == FeedStatus.DUE)
                .map(Animal::getName)
                .toList();
        List<String> overdue = animals.stream()
                .filter(animal -> {
                    FeedSchedule schedule = schedules.get(animal.getId());
                    return schedule.status() == FeedStatus.OVERDUE
                            && schedule.daysLeft() == -FeedReminder.OVERDUE_REMIND_DAYS;
                })
                .map(Animal::getName)
                .toList();
        return FeedReminder.body(dueToday, overdue);
    }
}
