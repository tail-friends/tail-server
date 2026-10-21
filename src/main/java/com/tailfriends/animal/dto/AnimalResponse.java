package com.tailfriends.animal.dto;

import com.tailfriends.animal.domain.Animal;
import com.tailfriends.animal.domain.FeedSchedule;
import com.tailfriends.animal.domain.Sex;
import java.time.LocalDate;

/**
 * 개체 상세. shed, weight, character, cooling, alerts, mood는 탈피·체중·캐릭터·경고 계산이 생기면 추가한다.
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
        FeedResponse feed
) {

    public static AnimalResponse of(Animal animal, FeedSchedule feed) {
        return new AnimalResponse(
                animal.getId(),
                animal.getName(),
                animal.getSpeciesName(),
                animal.getMorph(),
                animal.getSex(),
                animal.getHatchDate(),
                animal.getFeedIntervalDays(),
                animal.getMemo(),
                FeedResponse.from(feed));
    }
}
