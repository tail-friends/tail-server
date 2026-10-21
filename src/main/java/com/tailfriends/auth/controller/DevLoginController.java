package com.tailfriends.auth.controller;

import com.tailfriends.auth.dto.DevLoginRequest;
import com.tailfriends.auth.dto.DevLoginResponse;
import com.tailfriends.auth.service.DevLoginService;
import com.tailfriends.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로컬에서 소셜 로그인 키 없이 토큰을 받는다. dev.login.enabled=true일 때만 생긴다.
 */
@Tag(name = "Dev")
@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(name = "dev.login.enabled", havingValue = "true")
public class DevLoginController {

    private final DevLoginService devLoginService;

    @Operation(summary = "테스트 로그인 (로컬 전용)",
            description = "같은 name이면 같은 사용자. role에 ADMIN을 넣으면 관리자 API를 쓸 수 있다. 운영에는 없다.")
    @PostMapping("/api/v1/dev/login")
    public ApiResponse<DevLoginResponse> login(@Valid @RequestBody DevLoginRequest request) {
        return ApiResponse.success(devLoginService.login(request));
    }
}
