package com.tailfriends.admin.controller;

import com.tailfriends.admin.dto.AdminSpeciesGuideRequest;
import com.tailfriends.admin.dto.AdminSpeciesGuideResponse;
import com.tailfriends.admin.service.AdminSpeciesGuideService;
import com.tailfriends.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
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

    @Operation(summary = "사육 가이드 편집용 조회",
            description = "종별 사육 가이드 카드에 마지막 수정 날짜(updatedAt, KST)와 운영자 닉네임(updatedBy)을 더했다. "
                    + "작성 전이면 sections는 빈 목록, updatedAt·updatedBy는 null. "
                    + "이전 기록이라 운영자를 알 수 없거나 탈퇴했으면 updatedBy만 null.")
    @GetMapping("/api/v1/admin/species/{speciesId}/guide")
    public ApiResponse<AdminSpeciesGuideResponse> getGuide(@PathVariable Long speciesId) {
        return ApiResponse.success(adminSpeciesGuideService.getGuide(speciesId));
    }

    @Operation(summary = "사육 가이드 저장",
            description = "종의 가이드 전체를 보낸 섹션으로 바꾼다. 보내지 않은 섹션은 지운다. 응답은 편집용 조회와 같다. "
                    + "바뀐 섹션이 있을 때만 마지막 수정 날짜·운영자가 바뀐다.")
    @PutMapping("/api/v1/admin/species/{speciesId}/guide")
    public ApiResponse<AdminSpeciesGuideResponse> saveGuide(
            @PathVariable Long speciesId,
            @Valid @RequestBody AdminSpeciesGuideRequest request) {
        return ApiResponse.success(adminSpeciesGuideService.saveGuide(speciesId, request));
    }
}
