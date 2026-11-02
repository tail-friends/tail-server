package com.tailfriends.breeding.dto;

import com.tailfriends.animal.domain.Animal;
import com.tailfriends.breeding.domain.Breeding;
import com.tailfriends.breeding.domain.BreedingStatus;
import java.time.LocalDate;
import java.util.List;

/**
 * maleId·femaleId는 외부 개체거나 지운 개체면 null. 이름은 항상 있다.
 */
public record BreedingResponse(
        Long id,
        Long maleId,
        String maleName,
        Long femaleId,
        String femaleName,
        LocalDate pairedDate,
        BreedingStatus status,
        String memo,
        List<ClutchResponse> clutches,
        List<Long> hatchlingIds
) {

    public static BreedingResponse of(Breeding breeding, List<ClutchResponse> clutches, List<Long> hatchlingIds) {
        return new BreedingResponse(
                breeding.getId(),
                idOf(breeding.getMale()),
                breeding.currentMaleName(),
                idOf(breeding.getFemale()),
                breeding.currentFemaleName(),
                breeding.getPairedDate(),
                breeding.getStatus(),
                breeding.getMemo(),
                clutches,
                hatchlingIds);
    }

    private static Long idOf(Animal animal) {
        return animal == null ? null : animal.getId();
    }
}
