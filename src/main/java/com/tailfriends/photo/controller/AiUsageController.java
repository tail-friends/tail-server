package com.tailfriends.photo.controller;

import com.tailfriends.global.response.ApiResponse;
import com.tailfriends.photo.dto.AiUsageResponse;
import com.tailfriends.photo.service.AiUsageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Photo")
@RestController
@RequiredArgsConstructor
public class AiUsageController {

    private final AiUsageService aiUsageService;

    @Operation(summary = "오늘 남은 AI 분석 횟수",
            description = "캐릭터·사육장·건강 체크가 같이 쓰는 하루(KST) 한도. 남은 횟수는 limit - used. "
                    + "실패한 시도 한도까지 다 썼으면 used = limit. resetsAt은 다음 날 0시(KST)를 UTC로 준다.")
    @GetMapping("/api/v1/photos/ai-usage")
    public ApiResponse<AiUsageResponse> getTodayUsage() {
        return ApiResponse.success(aiUsageService.getTodayUsage());
    }
}
