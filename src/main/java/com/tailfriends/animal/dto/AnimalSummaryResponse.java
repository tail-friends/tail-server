package com.tailfriends.animal.dto;

import com.tailfriends.animal.domain.Animal;
import com.tailfriends.animal.domain.FeedSchedule;

/**
 * 개체 목록의 한 줄. alertCount는 열린(OPEN) 건강 경고 수. thumbnailUrl, latestWeight는 사진·체중 계산이 생기면 추가한다.
 */
public record AnimalSummaryResponse(
        Long id,
        String name,
        String species,
        String morph,
        FeedResponse feed,
        long alertCount
) {

    public static AnimalSummaryResponse of(Animal animal, FeedSchedule feed, long alertCount) {
        return new AnimalSummaryResponse(animal.getId(), animal.getName(), animal.getSpeciesName(), animal.getMorph(),
                FeedResponse.from(feed), alertCount);
    }
}
