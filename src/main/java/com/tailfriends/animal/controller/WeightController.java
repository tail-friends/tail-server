package com.tailfriends.animal.controller;

import com.tailfriends.animal.dto.WeightGraphResponse;
import com.tailfriends.animal.service.WeightService;
import com.tailfriends.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Animal")
@RestController
@RequiredArgsConstructor
public class WeightController {

    private final WeightService weightService;

    @Operation(summary = "체중 그래프", description = "기간 안 체중 기록, 날짜 오름차순. 기본은 오늘(KST)까지 최근 1년.")
    @GetMapping("/api/v1/animals/{animalId}/weights")
    public ApiResponse<WeightGraphResponse> getWeights(@AuthenticationPrincipal Long userId,
                                                       @PathVariable Long animalId,
                                                       @RequestParam(required = false) LocalDate from,
                                                       @RequestParam(required = false) LocalDate to) {
        return ApiResponse.success(weightService.getWeights(userId, animalId, from, to));
    }
}
