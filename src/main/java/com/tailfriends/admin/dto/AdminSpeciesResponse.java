package com.tailfriends.admin.dto;

import com.tailfriends.species.domain.Species;
import com.tailfriends.species.dto.SpeciesResponse;
import com.tailfriends.species.dto.SpeciesResponse.EnvRange;
import java.util.List;

/**
 * 공개 종 목록(SpeciesResponse)과 같은 형식에 hidden, displayOrder를 더했다.
 */
public record AdminSpeciesResponse(
        Long id,
        String name,
        String shape,
        boolean spiky,
        int feedIntervalDays,
        Integer shedIntervalDays,
        EnvRange envRange,
        String defaultHome,
        boolean hidden,
        String category,
        String englishName,
        String scientificName,
        List<String> aliases,
        int displayOrder
) {

    public static AdminSpeciesResponse from(Species species) {
        SpeciesResponse response = SpeciesResponse.from(species);
        return new AdminSpeciesResponse(
                response.id(),
                response.name(),
                response.shape(),
                response.spiky(),
                response.feedIntervalDays(),
                response.shedIntervalDays(),
                response.envRange(),
                response.defaultHome(),
                species.isHidden(),
                response.category(),
                response.englishName(),
                response.scientificName(),
                response.aliases(),
                species.getDisplayOrder());
    }
}
