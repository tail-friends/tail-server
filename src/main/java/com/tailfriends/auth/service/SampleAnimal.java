package com.tailfriends.auth.service;

import com.tailfriends.animal.domain.Animal;
import com.tailfriends.animal.domain.CharacterLook;
import com.tailfriends.animal.domain.PatternType;
import com.tailfriends.animal.domain.Sex;
import com.tailfriends.log.domain.Log;
import com.tailfriends.log.domain.LogDetails;
import com.tailfriends.log.domain.LogType;
import com.tailfriends.log.domain.ShedStage;
import com.tailfriends.log.domain.Stool;
import com.tailfriends.species.domain.Shape;
import com.tailfriends.user.domain.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 로컬 샘플 데이터의 개체 3마리. 화면에서 급여 상태가 서로 다르게 보이도록 기록을 만든다.
 *
 * <ul>
 *   <li>망고: 급여 여유(D-2), 체중 증가, 3주 전 탈피 완료</li>
 *   <li>콩이: 급여일 2일 지남, 최근 거부, 체중 감소, 묽은 변</li>
 *   <li>단비: 오늘 급여, 이틀 전부터 탈피 준비(BLUE)</li>
 * </ul>
 *
 * 기록일은 모두 오늘(한국 날짜) 기준 최근 60일 안이다.
 */
enum SampleAnimal {

    MANGO("망고", "레오파드 게코", "탠저린", Sex.FEMALE, 16, 4,
            new CharacterLook(null, Shape.GECKO, PatternType.SPOTS,
                    "#E0A33A", "#3A2A1E", "#F2E6C8", "#2B2B2B",
                    new BigDecimal("0.60"), new BigDecimal("0.80"), new BigDecimal("0.40"))) {
        @Override
        List<Log> logs(Animal animal, User user, LocalDate today) {
            Logs logs = new Logs(animal, user, today);
            for (int daysAgo = 54; daysAgo >= 2; daysAgo -= 4) {
                logs.feed(daysAgo, "귀뚜라미", 5, daysAgo == 26);
            }
            logs.feed(25, "귀뚜라미", 4, false);
            for (int week = 8; week >= 0; week--) {
                logs.weight(week * 7, new BigDecimal("48.0").add(BigDecimal.valueOf(8 - week)));
                logs.clean(week * 7 + 1, week % 4 == 0, Stool.NORMAL);
                logs.env(week * 7 + 3, "32.0", "26.0", 40);
            }
            logs.shed(24, ShedStage.BLUE, false);
            logs.shed(21, ShedStage.SHED, true);
            logs.note(21, "한 번에 깨끗하게 벗었어요");
            return logs.list();
        }
    },

    KONGI("콩이", "크레스티드 게코", "할리퀸", Sex.MALE, 9, 3,
            new CharacterLook(null, Shape.GECKO, PatternType.STRIPES,
                    "#B5793F", "#F0D9A8", "#E8C99A", "#3B2F2F",
                    new BigDecimal("0.70"), new BigDecimal("0.50"), new BigDecimal("0.60"))) {
        @Override
        List<Log> logs(Animal animal, User user, LocalDate today) {
            Logs logs = new Logs(animal, user, today);
            for (int daysAgo = 56; daysAgo >= 5; daysAgo -= 3) {
                logs.feed(daysAgo, "슈퍼푸드", 1, false);
            }
            logs.feed(2, "슈퍼푸드", 1, true);
            logs.feed(1, "귀뚜라미", 3, true);
            for (int week = 8; week >= 0; week--) {
                logs.weight(week * 7, new BigDecimal("38.0").subtract(new BigDecimal("0.5").multiply(BigDecimal.valueOf(8 - week))));
                logs.clean(week * 7 + 2, week % 4 == 0, week <= 1 ? Stool.LOOSE : Stool.NORMAL);
                logs.env(week * 7 + 4, "26.0", "22.0", 70);
            }
            logs.shed(40, ShedStage.SHED, true);
            logs.note(1, "이틀째 먹이를 거부해요. 변이 묽어요");
            return logs.list();
        }
    },

    DANBI("단비", "볼 파이톤", "노멀", Sex.UNKNOWN, 30, 10,
            new CharacterLook(null, Shape.SNAKE, PatternType.BANDS,
                    "#6B4A2B", "#C9A66B", "#EFE3C8", "#1E1E1E",
                    new BigDecimal("0.50"), new BigDecimal("0.30"), new BigDecimal("0.70"))) {
        @Override
        List<Log> logs(Animal animal, User user, LocalDate today) {
            Logs logs = new Logs(animal, user, today);
            for (int daysAgo = 50; daysAgo >= 10; daysAgo -= 10) {
                logs.feed(daysAgo, "랫", 1, false);
            }
            for (int week = 8; week >= 0; week -= 2) {
                logs.weight(week * 7, new BigDecimal("1180").add(BigDecimal.valueOf((8 - week) * 9L)));
            }
            for (int week = 7; week >= 0; week--) {
                logs.clean(week * 7 + 5, week % 2 == 0, week % 2 == 0 ? Stool.NORMAL : Stool.NONE);
                logs.env(week * 7 + 6, "31.0", "25.0", 60);
            }
            logs.shed(48, ShedStage.BLUE, false);
            logs.shed(44, ShedStage.SHED, true);
            logs.shed(2, ShedStage.BLUE, false);
            logs.note(2, "눈이 파래졌어요. 탈피 준비 중");
            return logs.list();
        }
    };

    private final String name;
    private final String speciesName;
    private final String morph;
    private final Sex sex;
    private final int ageMonths;
    private final int feedIntervalDays;
    private final CharacterLook look;

    SampleAnimal(String name, String speciesName, String morph, Sex sex, int ageMonths, int feedIntervalDays,
                 CharacterLook look) {
        this.name = name;
        this.speciesName = speciesName;
        this.morph = morph;
        this.sex = sex;
        this.ageMonths = ageMonths;
        this.feedIntervalDays = feedIntervalDays;
        this.look = look;
    }

    /**
     * 기록을 만들기만 하고 저장하지 않는다.
     */
    abstract List<Log> logs(Animal animal, User user, LocalDate today);

    String getName() {
        return name;
    }

    String getSpeciesName() {
        return speciesName;
    }

    String getMorph() {
        return morph;
    }

    Sex getSex() {
        return sex;
    }

    /**
     * 해칭일은 오늘부터 ageMonths개월 전이다.
     */
    LocalDate getHatchDate(LocalDate today) {
        return today.minusMonths(ageMonths);
    }

    int getFeedIntervalDays() {
        return feedIntervalDays;
    }

    CharacterLook getLook() {
        return look;
    }

    /**
     * 오늘부터 며칠 전(daysAgo)으로 기록일을 정해 Log.create로 만든다. 종류별 검증도 실제 기록과 같다.
     */
    private static final class Logs {

        private final Animal animal;
        private final User user;
        private final LocalDate today;
        private final List<Log> list = new ArrayList<>();

        private Logs(Animal animal, User user, LocalDate today) {
            this.animal = animal;
            this.user = user;
            this.today = today;
        }

        void feed(int daysAgo, String food, int count, boolean refused) {
            add(daysAgo, LogType.FEED, null,
                    new LogDetails(food, count, refused, !refused, null, null, null, null, null, null, null, null));
        }

        void shed(int daysAgo, ShedStage stage, boolean complete) {
            add(daysAgo, LogType.SHED, null,
                    new LogDetails(null, null, null, null, stage, complete, null, null, null, null, null, null));
        }

        void weight(int daysAgo, BigDecimal grams) {
            add(daysAgo, LogType.WEIGHT, null,
                    new LogDetails(null, null, null, null, null, null, grams, null, null, null, null, null));
        }

        void clean(int daysAgo, boolean full, Stool stool) {
            add(daysAgo, LogType.CLEAN, null,
                    new LogDetails(null, null, null, null, null, null, null, full, stool, null, null, null));
        }

        void env(int daysAgo, String hot, String cool, int humidity) {
            add(daysAgo, LogType.ENV, null, new LogDetails(null, null, null, null, null, null, null, null, null,
                    new BigDecimal(hot), new BigDecimal(cool), humidity));
        }

        void note(int daysAgo, String memo) {
            add(daysAgo, LogType.NOTE, memo, null);
        }

        List<Log> list() {
            return list;
        }

        private void add(int daysAgo, LogType type, String memo, LogDetails details) {
            list.add(Log.create(animal, user, null, type, today.minusDays(daysAgo), memo, details));
        }
    }
}
