package com.tailfriends.admin.service;

import com.tailfriends.admin.dto.AdminStatsResponse;
import com.tailfriends.admin.dto.AdminStatsResponse.Daily;
import com.tailfriends.admin.repository.AdminStatsRepository;
import com.tailfriends.admin.repository.DailyCount;
import com.tailfriends.global.util.KstDate;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminStatsService {

    private final AdminStatsRepository statsRepository;
    private final Clock clock;

    /**
     * 오늘(KST)을 포함한 최근 days일.
     */
    @Transactional(readOnly = true)
    public AdminStatsResponse getStats(int days) {
        LocalDate today = KstDate.today(clock);
        LocalDate firstDay = today.minusDays(days - 1L);
        Instant from = firstDay.atStartOfDay(KstDate.ZONE).toInstant();

        Map<LocalDate, Long> newUsers = byDate(statsRepository.countNewUsersByDay(from));
        Map<LocalDate, Long> newAnimals = byDate(statsRepository.countNewAnimalsByDay(from));
        List<Daily> daily = Stream.iterate(firstDay, date -> !date.isAfter(today), date -> date.plusDays(1))
                .map(date -> new Daily(date, newUsers.getOrDefault(date, 0L), newAnimals.getOrDefault(date, 0L)))
                .toList();

        return new AdminStatsResponse(statsRepository.countUsers(), statsRepository.countAnimals(), daily);
    }

    private static Map<LocalDate, Long> byDate(List<DailyCount> counts) {
        return counts.stream().collect(Collectors.toMap(count -> LocalDate.parse(count.getDay()), DailyCount::getCount));
    }
}
