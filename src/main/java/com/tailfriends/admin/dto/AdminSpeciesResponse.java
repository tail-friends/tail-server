package com.tailfriends.admin.dto;

import com.tailfriends.species.domain.Species;
import com.tailfriends.species.dto.SpeciesResponse;
import com.tailfriends.species.dto.SpeciesResponse.EnvRange;

/**
 * 공개 종 목록(SpeciesResponse)과 같은 형식에 hidden을 더했다.
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
        boolean hidden
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
                species.isHidden());
    }
}
