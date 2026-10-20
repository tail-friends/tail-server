package com.tailfriends.user.controller;

import com.tailfriends.global.response.ApiResponse;
import com.tailfriends.user.dto.MeResponse;
import com.tailfriends.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 경로는 노션 명세대로 /api/v1/auth/me다.
 */
@Tag(name = "Auth")
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "현재 로그인한 사용자 조회", description = "사용자 정보와 포인트 잔액, 개체 수.")
    @GetMapping("/api/v1/auth/me")
    public ApiResponse<MeResponse> getMe(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(userService.getMe(userId));
    }
}
