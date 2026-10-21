package com.tailfriends.admin.controller;

import com.tailfriends.admin.dto.AdminSpeciesGuideRequest;
import com.tailfriends.admin.service.AdminSpeciesGuideService;
import com.tailfriends.global.response.ApiResponse;
import com.tailfriends.species.dto.SpeciesGuideResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * ADMIN 권한만 호출할 수 있다. (SecurityConfig의 /api/v1/admin/**)
 */
@Tag(name = "Admin")
@RestController
@RequiredArgsConstructor
public class AdminSpeciesGuideController {

    private final AdminSpeciesGuideService adminSpeciesGuideService;

    @Operation(summary = "사육 가이드 저장",
            description = "종의 가이드 전체를 보낸 섹션으로 바꾼다. 보내지 않은 섹션은 지운다. 응답은 종별 사육 가이드 카드와 같다.")
    @PutMapping("/api/v1/admin/species/{speciesId}/guide")
    public ApiResponse<SpeciesGuideResponse> saveGuide(
            @PathVariable Long speciesId,
            @Valid @RequestBody AdminSpeciesGuideRequest request) {
        return ApiResponse.success(adminSpeciesGuideService.saveGuide(speciesId, request));
    }
}
