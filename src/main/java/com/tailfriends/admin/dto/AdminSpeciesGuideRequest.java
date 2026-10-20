package com.tailfriends.admin.dto;

import com.tailfriends.species.domain.GuideSection;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 종의 가이드 전체. 보내지 않은 섹션은 지운다.
 */
public record AdminSpeciesGuideRequest(
        @NotNull @Size(max = 4) List<@NotNull @Valid Section> sections
) {

    public record Section(
            @NotNull GuideSection key,
            @NotBlank @Size(max = 2000) String body
    ) {
    }
}
