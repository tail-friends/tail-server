package com.tailfriends.species.dto;

import com.tailfriends.global.util.KstDate;
import com.tailfriends.species.domain.GuideSection;
import com.tailfriends.species.domain.Species;
import com.tailfriends.species.domain.SpeciesGuide;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public record SpeciesGuideResponse(
        Long speciesId,
        String name,
        List<Section> sections
) {

    /**
     * 섹션은 습성·먹이·환경·건강 순서로 담는다.
     */
    public static SpeciesGuideResponse of(Species species, List<SpeciesGuide> guides) {
        return new SpeciesGuideResponse(
                species.getId(),
                species.getName(),
                guides.stream()
                        .sorted(Comparator.comparing(SpeciesGuide::getSection))
                        .map(Section::from)
                        .toList());
    }

    public record Section(
            GuideSection key,
            String title,
            String body,
            LocalDate updatedAt
    ) {

        static Section from(SpeciesGuide guide) {
            return new Section(
                    guide.getSection(),
                    guide.getSection().getTitle(),
                    guide.getBody(),
                    LocalDate.ofInstant(guide.getUpdatedAt(), KstDate.ZONE));
        }
    }
}
