package com.tailfriends.health.controller;

import com.tailfriends.global.response.ApiResponse;
import com.tailfriends.health.domain.AlertStatus;
import com.tailfriends.health.dto.AlertListResponse;
import com.tailfriends.health.dto.AlertResponse;
import com.tailfriends.health.dto.AlertUpdateRequest;
import com.tailfriends.health.service.HealthAlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Health")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/health/alerts")
public class HealthAlertController {

    private final HealthAlertService healthAlertService;

    @Operation(summary = "건강 경고 목록", description = "최신순. animalId가 없으면 내 전체 개체. cursor는 이전 응답의 nextCursor.")
    @GetMapping
    public ApiResponse<AlertListResponse> getAlerts(@RequestParam(required = false) Long animalId,
                                                    @RequestParam(required = false) AlertStatus status,
                                                    @RequestParam(required = false) Long cursor,
                                                    @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(healthAlertService.getAlerts(animalId, status, cursor, size));
    }

    @Operation(summary = "건강 경고 확인 처리", description = "status는 ACKNOWLEDGED만 된다.")
    @PatchMapping("/{alertId}")
    public ApiResponse<AlertResponse> update(@PathVariable Long alertId,
                                             @Valid @RequestBody AlertUpdateRequest request) {
        return ApiResponse.success(healthAlertService.update(alertId, request));
    }
}
