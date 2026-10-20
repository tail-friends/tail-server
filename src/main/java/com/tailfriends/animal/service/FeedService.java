package com.tailfriends.animal.service;

import com.tailfriends.animal.domain.Animal;
import com.tailfriends.animal.domain.FeedSchedule;
import com.tailfriends.global.util.KstDate;
import com.tailfriends.log.repository.LastFedDate;
import com.tailfriends.log.repository.LogRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 다음 급여일 계산. 오늘은 한국 날짜 기준이다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedService {

    private final LogRepository logRepository;
    private final Clock clock;

    public FeedSchedule scheduleOf(Animal animal) {
        LocalDate lastFedDate = logRepository.findLastFedDate(animal.getId()).orElse(null);
        return FeedSchedule.of(animal, lastFedDate, KstDate.today(clock));
    }

    /**
     * 개체 ID별 급여 상태. 개체 수와 상관없이 쿼리 한 번으로 조회한다.
     */
    public Map<Long, FeedSchedule> schedulesOf(List<Animal> animals) {
        if (animals.isEmpty()) {
            return Map.of();
        }
        Map<Long, LocalDate> lastFedDates = logRepository.findLastFedDates(animals.stream().map(Animal::getId).toList())
                .stream()
                .collect(Collectors.toMap(LastFedDate::animalId, LastFedDate::date));
        LocalDate today = KstDate.today(clock);
        return animals.stream().collect(Collectors.toMap(Animal::getId,
                animal -> FeedSchedule.of(animal, lastFedDates.get(animal.getId()), today),
                (first, second) -> first));
    }
}
