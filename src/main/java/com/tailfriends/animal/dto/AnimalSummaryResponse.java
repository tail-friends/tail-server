package com.tailfriends.animal.dto;

import com.tailfriends.animal.domain.Animal;

/**
 * 개체 목록의 한 줄. thumbnailUrl, feed, latestWeight, alertCount는 사진·기록·경고 테이블이 생기면 추가한다.
 */
public record AnimalSummaryResponse(
        Long id,
        String name,
        String species,
        String morph
) {

    public static AnimalSummaryResponse from(Animal animal) {
        return new AnimalSummaryResponse(animal.getId(), animal.getName(), animal.getSpeciesName(), animal.getMorph());
    }
}
