package com.tailfriends.species.controller;

import com.tailfriends.global.response.ApiResponse;
import com.tailfriends.species.domain.GuideSection;
import com.tailfriends.species.dto.SpeciesGuideResponse;
import com.tailfriends.species.service.SpeciesGuideService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Species")
@RestController
@RequiredArgsConstructor
public class SpeciesGuideController {

    private final SpeciesGuideService speciesGuideService;

    @Operation(summary = "종별 사육 가이드 카드",
            description = "습성·먹이·환경·건강 섹션. section을 주면 그 섹션만. 작성된 섹션이 없으면 sections는 빈 목록이다.")
    @GetMapping("/api/v1/species/{speciesId}/guide")
    public ApiResponse<SpeciesGuideResponse> getGuide(
            @PathVariable Long speciesId,
            @RequestParam(required = false) GuideSection section) {
        return ApiResponse.success(speciesGuideService.getGuide(speciesId, section));
    }
}
