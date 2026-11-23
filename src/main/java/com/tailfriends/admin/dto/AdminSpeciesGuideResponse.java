package com.tailfriends.admin.dto;

import com.tailfriends.species.domain.Species;
import com.tailfriends.species.domain.SpeciesGuide;
import com.tailfriends.species.dto.SpeciesGuideResponse;
import com.tailfriends.species.dto.SpeciesGuideResponse.Section;
import java.time.LocalDate;
import java.util.List;

/**
 * 종별 사육 가이드 카드(SpeciesGuideResponse)와 같은 형식에 가이드 작성 현황을 더했다.
 *
 * @param updatedAt 가이드를 마지막으로 바꾼 날(KST). 작성 전이면 null
 * @param updatedBy 가이드를 마지막으로 바꾼 운영자 닉네임. 알 수 없거나 탈퇴했으면 null
 */
public record AdminSpeciesGuideResponse(
        Long speciesId,
        String name,
        List<Section> sections,
        LocalDate updatedAt,
        String updatedBy
) {

    public static AdminSpeciesGuideResponse of(Species species, List<SpeciesGuide> guides) {
        SpeciesGuideResponse guide = SpeciesGuideResponse.of(species, guides);
        return new AdminSpeciesGuideResponse(
                guide.speciesId(),
                guide.name(),
                guide.sections(),
                AdminSpeciesResponse.guideUpdatedDate(species),
                AdminSpeciesResponse.guideEditorNickname(species));
    }
}
