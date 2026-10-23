package com.tailfriends.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.tailfriends.animal.domain.Animal;
import com.tailfriends.animal.domain.FeedSchedule;
import com.tailfriends.animal.domain.FeedStatus;
import com.tailfriends.log.domain.Log;
import com.tailfriends.log.domain.LogType;
import com.tailfriends.log.domain.ShedStage;
import com.tailfriends.user.domain.Provider;
import com.tailfriends.user.domain.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class SampleAnimalTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 22);
    private static final User USER = User.builder().provider(Provider.DEV).providerId("tester").nickname("tester").build();

    @ParameterizedTest
    @EnumSource(SampleAnimal.class)
    void 기록일은_오늘부터_60일_전까지다(SampleAnimal sample) {
        assertThat(logsOf(sample))
                .isNotEmpty()
                .allSatisfy(log -> assertThat(log.getLogDate()).isBetween(TODAY.minusDays(60), TODAY));
    }

    @ParameterizedTest
    @EnumSource(SampleAnimal.class)
    void 모든_종류의_기록이_있다(SampleAnimal sample) {
        assertThat(logsOf(sample)).extracting(Log::getType).contains(LogType.values());
    }

    @Test
    void 망고는_급여일이_이틀_남았다() {
        assertFeed(SampleAnimal.MANGO, FeedStatus.UPCOMING, 2);
    }

    @Test
    void 콩이는_급여일이_이틀_지났다() {
        assertFeed(SampleAnimal.KONGI, FeedStatus.OVERDUE, -2);
    }

    @Test
    void 단비는_오늘_급여일이다() {
        assertFeed(SampleAnimal.DANBI, FeedStatus.DUE, 0);
    }

    @Test
    void 콩이는_최근_기록이_거부라_마지막_급여일에서_빠진다() {
        List<Log> feeds = logsOf(SampleAnimal.KONGI).stream()
                .filter(log -> log.getType() == LogType.FEED)
                .sorted(Comparator.comparing(Log::getLogDate).reversed())
                .toList();

        assertThat(feeds.get(0).getRefused()).isTrue();
        assertThat(feeds.get(1).getRefused()).isTrue();
    }

    @Test
    void 콩이는_체중이_줄고_망고는_체중이_는다() {
        assertThat(firstAndLastWeight(SampleAnimal.KONGI)).satisfies(w -> assertThat(w[1]).isLessThan(w[0]));
        assertThat(firstAndLastWeight(SampleAnimal.MANGO)).satisfies(w -> assertThat(w[1]).isGreaterThan(w[0]));
    }

    @Test
    void 단비의_마지막_탈피_기록은_완료되지_않은_BLUE다() {
        Log latestShed = logsOf(SampleAnimal.DANBI).stream()
                .filter(log -> log.getType() == LogType.SHED)
                .max(Comparator.comparing(Log::getLogDate))
                .orElseThrow();

        assertThat(latestShed.getShedStage()).isEqualTo(ShedStage.BLUE);
        assertThat(latestShed.getShedComplete()).isFalse();
    }

    private static void assertFeed(SampleAnimal sample, FeedStatus status, int daysLeft) {
        Animal animal = animalOf(sample);
        LocalDate lastFed = sample.logs(animal, USER, TODAY).stream()
                .filter(log -> log.getType() == LogType.FEED && !log.getRefused())
                .map(Log::getLogDate)
                .max(Comparator.naturalOrder())
                .orElseThrow();

        FeedSchedule schedule = FeedSchedule.of(animal, lastFed, TODAY);

        assertThat(schedule.status()).isEqualTo(status);
        assertThat(schedule.daysLeft()).isEqualTo(daysLeft);
    }

    private static BigDecimal[] firstAndLastWeight(SampleAnimal sample) {
        List<Log> weights = logsOf(sample).stream()
                .filter(log -> log.getType() == LogType.WEIGHT)
                .sorted(Comparator.comparing(Log::getLogDate))
                .toList();
        return new BigDecimal[] {weights.get(0).getWeightG(), weights.get(weights.size() - 1).getWeightG()};
    }

    private static List<Log> logsOf(SampleAnimal sample) {
        return sample.logs(animalOf(sample), USER, TODAY);
    }

    private static Animal animalOf(SampleAnimal sample) {
        return Animal.builder()
                .user(USER)
                .speciesName(sample.getSpeciesName())
                .name(sample.getName())
                .feedIntervalDays(sample.getFeedIntervalDays())
                .build();
    }
}
