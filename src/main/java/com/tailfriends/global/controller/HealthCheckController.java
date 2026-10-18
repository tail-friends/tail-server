package com.tailfriends.global.controller;

import com.tailfriends.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Health Check")
@RestController
@RequestMapping("/api/v1")
public class HealthCheckController {

    @Operation(summary = "서버 상태 확인")
    @GetMapping("/health-check")
    public ApiResponse<String> healthCheck() {
        return ApiResponse.success("ok");
    }
}
