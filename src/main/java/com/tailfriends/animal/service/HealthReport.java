package com.tailfriends.animal.service;

import com.tailfriends.animal.domain.Sex;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 수의사용 건강 리포트(F-17)에 담을 내용. 기간(from, to)은 양끝을 포함한 한국 날짜다.
 *
 * @param weights 기간 안 체중 기록, 날짜 오름차순
 * @param visits  기간 안 진료 기록, 방문일 최신순
 */
public record HealthReport(
        LocalDate generatedOn,
        LocalDate from,
        LocalDate to,
        Profile profile,
        List<WeightEntry> weights,
        FeedSummary feed,
        ShedSummary shed,
        List<VisitEntry> visits
) {

    public record Profile(String name, String species, String morph, Sex sex, LocalDate hatchDate,
                          int feedIntervalDays) {
    }

    public record WeightEntry(LocalDate date, BigDecimal grams) {
    }

    /**
     * @param total          급여 기록 수 (먹음 + 안 먹음)
     * @param refused        "안 먹음" 기록 수
     * @param lastEatenDate  기간 안에서 마지막으로 먹은 날. 없으면 null
     * @param refusalStreak  기간 끝에서부터 연속된 "안 먹음" 수
     */
    public record FeedSummary(int total, int refused, LocalDate lastEatenDate, int refusalStreak) {

        public int eaten() {
            return total - refused;
        }
    }

    /**
     * @param complete     완탈 수
     * @param partial      부분 탈피 수
     * @param blue         블루 단계 기록 수
     * @param lastShedDate 기간 안 마지막 탈피일(블루 단계 제외). 없으면 null
     */
    public record ShedSummary(int complete, int partial, int blue, LocalDate lastShedDate) {

        public int sheds() {
            return complete + partial;
        }
    }

    public record VisitEntry(LocalDate visitDate, String hospital, String symptom, String diagnosis,
                             String prescription) {
    }
}
