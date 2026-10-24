package com.tailfriends.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.tailfriends.admin.dto.AdminStatsResponse;
import com.tailfriends.admin.dto.AdminStatsResponse.Daily;
import com.tailfriends.admin.repository.AdminStatsRepository;
import com.tailfriends.admin.repository.DailyCount;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class AdminStatsServiceTest {

    // UTC 2026-10-19 15:30 = KST 2026-10-20 00:30
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-19T15:30:00Z"), ZoneOffset.UTC);
    // KST 2026-10-18 00:00
    private static final Instant FROM = Instant.parse("2026-10-17T15:00:00Z");

    private final AdminStatsRepository statsRepository = mock(AdminStatsRepository.class);
    private final AdminStatsService service = new AdminStatsService(statsRepository, CLOCK);

    @Test
    void 오늘을_포함한_최근_n일을_한국_날짜로_묶고_빈_날은_0으로_채운다() {
        given(statsRepository.countUsers()).willReturn(120L);
        given(statsRepository.countAnimals()).willReturn(340L);
        given(statsRepository.countNewUsersByDay(FROM)).willReturn(List.of(count("2026-10-18", 3), count("2026-10-20", 1)));
        given(statsRepository.countNewAnimalsByDay(FROM)).willReturn(List.of(count("2026-10-19", 5)));

        AdminStatsResponse response = service.getStats(3);

        assertThat(response.totalUsers()).isEqualTo(120);
        assertThat(response.totalAnimals()).isEqualTo(340);
        assertThat(response.daily()).containsExactly(
                new Daily(LocalDate.of(2026, 10, 18), 3, 0, 0, 0),
                new Daily(LocalDate.of(2026, 10, 19), 0, 5, 0, 0),
                new Daily(LocalDate.of(2026, 10, 20), 1, 0, 0, 0));
    }

    private static DailyCount count(String day, long count) {
        return new DailyCount() {
            @Override
            public String getDay() {
                return day;
            }

            @Override
            public Long getCount() {
                return count;
            }
        };
    }
}
