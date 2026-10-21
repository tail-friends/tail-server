package com.tailfriends.user.controller;

import com.tailfriends.global.response.ApiResponse;
import com.tailfriends.user.dto.ConsentRequest;
import com.tailfriends.user.dto.ConsentResponse;
import com.tailfriends.user.service.ConsentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 경로는 노션 명세대로 /api/v1/auth/consents다.
 */
@Tag(name = "Auth")
@RestController
@RequiredArgsConstructor
public class ConsentController {

    private final ConsentService consentService;

    @Operation(summary = "개인정보 동의 조회", description = "항목별 현재 동의 상태. 답하지 않은 항목은 false.")
    @GetMapping("/api/v1/auth/consents")
    public ApiResponse<ConsentResponse> getConsents(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(consentService.getConsents(userId));
    }

    @Operation(summary = "개인정보 동의 저장",
            description = "네 항목을 모두 보낸다. privacy·photoAnalysis는 필수(true만). 바뀐 항목만 이력으로 남긴다.")
    @PutMapping("/api/v1/auth/consents")
    public ApiResponse<ConsentResponse> saveConsents(
            @AuthenticationPrincipal Long userId, @Valid @RequestBody ConsentRequest request) {
        return ApiResponse.success(consentService.saveConsents(userId, request));
    }
}
