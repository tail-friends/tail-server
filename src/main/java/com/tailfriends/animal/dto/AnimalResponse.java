package com.tailfriends.animal.dto;

import com.tailfriends.animal.domain.Animal;
import com.tailfriends.animal.domain.FeedSchedule;
import com.tailfriends.animal.domain.Sex;
import com.tailfriends.health.dto.AlertResponse;
import java.time.LocalDate;
import java.util.List;

/**
 * 개체 상세. alerts는 열린(OPEN) 건강 경고, 최신순. shed, weight, character, cooling, mood는 탈피·체중·캐릭터 계산이 생기면 추가한다.
 */
public record AnimalResponse(
        Long id,
        String name,
        String species,
        String morph,
        Sex sex,
        LocalDate hatchDate,
        int feedIntervalDays,
        String memo,
        FeedResponse feed,
        List<AlertResponse> alerts
) {

    public static AnimalResponse of(Animal animal, FeedSchedule feed, List<AlertResponse> alerts) {
        return new AnimalResponse(
                animal.getId(),
                animal.getName(),
                animal.getSpeciesName(),
                animal.getMorph(),
                animal.getSex(),
                animal.getHatchDate(),
                animal.getFeedIntervalDays(),
                animal.getMemo(),
                FeedResponse.from(feed),
                alerts);
    }
}
