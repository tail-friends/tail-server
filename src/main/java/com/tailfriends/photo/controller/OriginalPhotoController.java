package com.tailfriends.photo.controller;

import com.tailfriends.global.response.ApiResponse;
import com.tailfriends.photo.dto.OriginalDeleteResponse;
import com.tailfriends.photo.service.OriginalPhotoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 경로는 노션 명세대로 /api/v1/users/me/originals다.
 */
@Tag(name = "Photo")
@RestController
@RequiredArgsConstructor
public class OriginalPhotoController {

    private final OriginalPhotoService originalPhotoService;

    @Operation(summary = "보관한 원본 사진 모두 삭제",
            description = "보관 중인 원본 사진을 모두 지우고 keepOriginalDefault(원본 사진 보관 동의)를 false로 바꾼다. 썸네일은 남는다.")
    @DeleteMapping("/api/v1/users/me/originals")
    public ApiResponse<OriginalDeleteResponse> deleteAll() {
        return ApiResponse.success(originalPhotoService.deleteAll());
    }
}
