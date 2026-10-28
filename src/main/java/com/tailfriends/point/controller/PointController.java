package com.tailfriends.point.controller;

import com.tailfriends.global.response.ApiResponse;
import com.tailfriends.point.dto.PointHistoryListResponse;
import com.tailfriends.point.service.PointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Point")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/points")
public class PointController {

    private final PointService pointService;

    @Operation(summary = "포인트 내역", description = "잔액과 적립·사용 내역(최신순). cursor는 이전 응답의 nextCursor.")
    @GetMapping("/history")
    public ApiResponse<PointHistoryListResponse> getHistory(@RequestParam(required = false) Long cursor,
                                                            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(pointService.getHistory(cursor, size));
    }
}
