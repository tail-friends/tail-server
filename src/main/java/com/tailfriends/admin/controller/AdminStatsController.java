package com.tailfriends.admin.controller;

import com.tailfriends.admin.dto.AdminStatsResponse;
import com.tailfriends.admin.service.AdminStatsService;
import com.tailfriends.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * ADMIN 권한만 호출할 수 있다. (SecurityConfig의 /api/v1/admin/**)
 */
@Tag(name = "Admin")
@RestController
@RequiredArgsConstructor
public class AdminStatsController {

    private final AdminStatsService adminStatsService;

    @Operation(summary = "운영 통계",
            description = "전체 가입자·개체 수와 최근 days일(오늘 포함, KST)의 날짜별 새 가입자·새 개체 수, AI 분석 수·실패 수. "
                    + "days는 1~90, 기본 7.")
    @GetMapping("/api/v1/admin/stats")
    public ApiResponse<AdminStatsResponse> getStats(
            @RequestParam(defaultValue = "7") @Min(1) @Max(90) int days) {
        return ApiResponse.success(adminStatsService.getStats(days));
    }
}
