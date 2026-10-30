package com.tailfriends.animal.dto;

import com.tailfriends.animal.domain.Animal;
import com.tailfriends.animal.domain.FeedSchedule;
import com.tailfriends.animal.domain.Mood;
import com.tailfriends.animal.domain.Sex;
import com.tailfriends.animal.domain.ShedSummary;
import com.tailfriends.animal.service.AnimalStatusService.AnimalStatus;
import com.tailfriends.health.dto.AlertResponse;
import java.time.LocalDate;
import java.util.List;

/**
 * 개체 상세. alerts는 열린(OPEN) 건강 경고, 최신순. character는 저장한 캐릭터가 없으면 null.
 * cooling은 정해 둔 쿨링 기간이고, 정하지 않았으면 null.
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
        ShedSummary shed,
        WeightSummaryResponse weight,
        CharacterSummaryResponse character,
        CoolingResponse cooling,
        List<AlertResponse> alerts,
        Mood mood
) {

    public static AnimalResponse of(Animal animal, FeedSchedule feed, List<AlertResponse> alerts, AnimalStatus status) {
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
                status.shed(),
                status.weight(),
                status.character(),
                CoolingResponse.of(animal),
                alerts,
                status.mood());
    }
}
