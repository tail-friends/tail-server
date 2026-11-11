package com.tailfriends.animal.service;

import com.tailfriends.animal.domain.Animal;
import com.tailfriends.animal.repository.AnimalRepository;
import com.tailfriends.animal.service.HealthReport.FeedSummary;
import com.tailfriends.animal.service.HealthReport.Profile;
import com.tailfriends.animal.service.HealthReport.ShedSummary;
import com.tailfriends.animal.service.HealthReport.VisitEntry;
import com.tailfriends.animal.service.HealthReport.WeightEntry;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.security.CurrentUser;
import com.tailfriends.global.util.KstDate;
import com.tailfriends.health.domain.VetVisit;
import com.tailfriends.health.repository.VetVisitRepository;
import com.tailfriends.log.domain.Log;
import com.tailfriends.log.domain.LogType;
import com.tailfriends.log.domain.ShedStage;
import com.tailfriends.log.repository.LogRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 개체별 수의사용 건강 리포트 PDF(F-17). 최근 n개월의 체중 그래프, 급여·거식·탈피 요약, 진료 기록을 담는다.
 */
@Service
@RequiredArgsConstructor
public class HealthReportService {

    private final CurrentUser currentUser;
    private final AnimalRepository animalRepository;
    private final LogRepository logRepository;
    private final VetVisitRepository vetVisitRepository;
    private final HealthReportPdfWriter pdfWriter;
    private final Clock clock;

    /**
     * 오늘(KST)까지 최근 months개월의 리포트를 PDF로 만든다. 기간은 (오늘 - months개월) ~ 오늘, 양끝 포함.
     *
     * @throws BusinessException 개체가 없거나 내 개체가 아니면 ANIMAL_NOT_FOUND
     */
    @Transactional(readOnly = true)
    public byte[] createReport(Long animalId, int months) {
        Animal animal = animalRepository.findByIdAndUserId(animalId, currentUser.id())
                .orElseThrow(() -> new BusinessException(ErrorCode.ANIMAL_NOT_FOUND));
        LocalDate today = KstDate.today(clock);
        LocalDate from = today.minusMonths(months);
        List<Log> logs = logRepository.findAllByAnimalIdAndLogDateBetweenOrderByLogDateAscIdAsc(animalId, from, today);
        List<VetVisit> visits = vetVisitRepository.findAllByAnimalIdOrderByVisitDateDescIdDesc(animalId);
        return pdfWriter.write(summarize(animal, from, today, logs, visits));
    }

    /**
     * 기간 안 기록과 진료 기록을 리포트 내용으로 모은다.
     *
     * @param logs   기간 안 기록, 기록일 오름차순
     * @param visits 개체의 진료 기록, 방문일 최신순. 기간 밖은 뺀다
     */
    static HealthReport summarize(Animal animal, LocalDate from, LocalDate to, List<Log> logs, List<VetVisit> visits) {
        Profile profile = new Profile(animal.getName(), animal.getSpeciesName(), animal.getMorph(), animal.getSex(),
                animal.getHatchDate(), animal.getFeedIntervalDays());
        List<WeightEntry> weights = logs.stream()
                .filter(log -> log.getType() == LogType.WEIGHT && log.getWeightG() != null)
                .map(log -> new WeightEntry(log.getLogDate(), log.getWeightG()))
                .toList();
        List<VisitEntry> visitEntries = visits.stream()
                .filter(visit -> !visit.getVisitDate().isBefore(from) && !visit.getVisitDate().isAfter(to))
                .map(visit -> new VisitEntry(visit.getVisitDate(), visit.getHospital(), visit.getSymptom(),
                        visit.getDiagnosis(), visit.getPrescription()))
                .toList();
        return new HealthReport(to, from, to, profile, weights, feedSummary(logs), shedSummary(logs), visitEntries);
    }

    private static FeedSummary feedSummary(List<Log> logs) {
        List<Log> feeds = logs.stream().filter(log -> log.getType() == LogType.FEED).toList();
        int refused = 0;
        LocalDate lastEaten = null;
        int streak = 0;
        for (Log feed : feeds) {
            if (Boolean.TRUE.equals(feed.getRefused())) {
                refused++;
                streak++;
            } else {
                lastEaten = feed.getLogDate();
                streak = 0;
            }
        }
        return new FeedSummary(feeds.size(), refused, lastEaten, streak);
    }

    private static ShedSummary shedSummary(List<Log> logs) {
        int complete = 0;
        int partial = 0;
        int blue = 0;
        LocalDate lastShed = null;
        for (Log log : logs) {
            if (log.getType() != LogType.SHED) {
                continue;
            }
            if (log.getShedStage() == ShedStage.BLUE) {
                blue++;
                continue;
            }
            if (Boolean.TRUE.equals(log.getShedComplete())) {
                complete++;
            } else {
                partial++;
            }
            lastShed = log.getLogDate();
        }
        return new ShedSummary(complete, partial, blue, lastShed);
    }
}
