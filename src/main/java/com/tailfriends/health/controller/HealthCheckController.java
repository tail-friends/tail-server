package com.tailfriends.health.controller;

import com.tailfriends.global.response.ApiResponse;
import com.tailfriends.health.dto.HealthCheckListResponse;
import com.tailfriends.health.dto.HealthCheckResponse;
import com.tailfriends.health.service.HealthCheckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Health")
@RestController
@RequiredArgsConstructor
public class HealthCheckController {

    private final HealthCheckService healthCheckService;

    @Operation(summary = "사진 건강 체크 (AI)",
            description = "건강 체크용(HEALTH)으로 올린 사진을 항목별(RETAINED_SHED·EYES·TAIL(게코만)·BODY·SKIN)로 "
                    + "GOOD·ATTENTION·UNKNOWN 판정하고 저장한다. ATTENTION이 있으면 건강 경고를 연다. "
                    + "AI 하루 횟수는 캐릭터 분석과 함께 센다.")
    @PostMapping("/api/v1/photos/{photoId}/health-check")
    public ApiResponse<HealthCheckResponse> check(@PathVariable Long photoId) {
        return ApiResponse.success(healthCheckService.check(photoId));
    }

    @Operation(summary = "건강 체크 결과 목록", description = "개체의 건강 체크 기록, 최신순. cursor는 이전 응답의 nextCursor.")
    @GetMapping("/api/v1/health/checks")
    public ApiResponse<HealthCheckListResponse> getChecks(@RequestParam Long animalId,
                                                         @RequestParam(required = false) Long cursor,
                                                         @RequestParam(defaultValue = "20") @Min(1) @Max(100)
                                                         int size) {
        return ApiResponse.success(healthCheckService.getChecks(animalId, cursor, size));
    }
}
