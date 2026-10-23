package com.tailfriends.admin.controller;

import com.tailfriends.admin.dto.AdminSpeciesCreateRequest;
import com.tailfriends.admin.dto.AdminSpeciesListResponse;
import com.tailfriends.admin.dto.AdminSpeciesResponse;
import com.tailfriends.admin.dto.AdminSpeciesUpdateRequest;
import com.tailfriends.admin.service.AdminSpeciesService;
import com.tailfriends.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * ADMIN 권한만 호출할 수 있다. (SecurityConfig의 /api/v1/admin/**)
 */
@Tag(name = "Admin")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/species")
public class AdminSpeciesController {

    private final AdminSpeciesService adminSpeciesService;

    @Operation(summary = "종 목록 (관리용)", description = "숨긴 종까지 모두, hidden 포함.")
    @GetMapping
    public ApiResponse<AdminSpeciesListResponse> getSpeciesList() {
        return ApiResponse.success(adminSpeciesService.getSpeciesList());
    }

    @Operation(summary = "종 추가", description = "이름이 겹치면 400. 온습도 범위의 최소가 최대보다 크면 400.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AdminSpeciesResponse> create(@Valid @RequestBody AdminSpeciesCreateRequest request) {
        return ApiResponse.success(adminSpeciesService.create(request));
    }

    @Operation(summary = "종 수정", description = "보낸 필드만 바꾼다. hidden=false로 다시 보이게 할 수 있다.")
    @PatchMapping("/{speciesId}")
    public ApiResponse<AdminSpeciesResponse> update(@PathVariable Long speciesId,
                                                    @Valid @RequestBody AdminSpeciesUpdateRequest request) {
        return ApiResponse.success(adminSpeciesService.update(speciesId, request));
    }

    @Operation(summary = "종 숨기기", description = "삭제 대신 숨긴다. 새 등록 목록에서만 빠지고 이미 등록된 개체는 그대로. 본문 없이 204.")
    @DeleteMapping("/{speciesId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void hide(@PathVariable Long speciesId) {
        adminSpeciesService.hide(speciesId);
    }
}
