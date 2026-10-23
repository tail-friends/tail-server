package com.tailfriends.user.controller;

import com.tailfriends.global.response.ApiResponse;
import com.tailfriends.user.dto.SettingsResponse;
import com.tailfriends.user.dto.SettingsUpdateRequest;
import com.tailfriends.user.service.UserSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User")
@RestController
@RequiredArgsConstructor
public class UserSettingsController {

    private final UserSettingsService userSettingsService;

    @Operation(summary = "설정 조회", description = "바꾼 적이 없으면 기본값. 알림 시간은 HH:mm.")
    @GetMapping("/api/v1/users/me/settings")
    public ApiResponse<SettingsResponse> getSettings() {
        return ApiResponse.success(userSettingsService.getSettings());
    }

    @Operation(summary = "설정 수정",
            description = "보낸 필드만 바꾼다. keepOriginalDefault·pushEnabled는 원본 사진 보관·알림 수신 동의를 바꾼다.")
    @PatchMapping("/api/v1/users/me/settings")
    public ApiResponse<SettingsResponse> updateSettings(
            @Valid @RequestBody SettingsUpdateRequest request) {
        return ApiResponse.success(userSettingsService.updateSettings(request));
    }
}
