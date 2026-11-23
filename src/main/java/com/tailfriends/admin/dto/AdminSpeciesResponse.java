package com.tailfriends.admin.dto;

import com.tailfriends.global.util.KstDate;
import com.tailfriends.species.domain.Species;
import com.tailfriends.species.dto.SpeciesResponse;
import com.tailfriends.species.dto.SpeciesResponse.EnvRange;
import com.tailfriends.user.domain.User;
import java.time.LocalDate;
import java.util.List;

/**
 * 공개 종 목록(SpeciesResponse)과 같은 형식에 hidden, displayOrder와 가이드 작성 현황을 더했다.
 *
 * @param guideUpdatedAt 가이드를 마지막으로 바꾼 날(KST). 작성 전이면 null
 * @param guideUpdatedBy 가이드를 마지막으로 바꾼 운영자 닉네임. 알 수 없거나 탈퇴했으면 null
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
        int displayOrder,
        LocalDate guideUpdatedAt,
        String guideUpdatedBy
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
                species.getDisplayOrder(),
                guideUpdatedDate(species),
                guideEditorNickname(species));
    }

    static LocalDate guideUpdatedDate(Species species) {
        return species.getGuideUpdatedAt() == null
                ? null
                : LocalDate.ofInstant(species.getGuideUpdatedAt(), KstDate.ZONE);
    }

    static String guideEditorNickname(Species species) {
        User editor = species.getGuideUpdatedBy();
        return editor == null ? null : editor.getNickname();
    }
}
