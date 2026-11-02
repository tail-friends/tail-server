package com.tailfriends.breeding.controller;

import com.tailfriends.breeding.dto.BreedingListResponse;
import com.tailfriends.breeding.service.BreedingService;
import com.tailfriends.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Breeding")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/breeding")
public class BreedingController {

    private final BreedingService breedingService;

    @Operation(summary = "번식 기록 목록", description = "메이팅일 최신순. 페이지를 나누지 않는다.")
    @GetMapping
    public ApiResponse<BreedingListResponse> getBreedings() {
        return ApiResponse.success(breedingService.getBreedings());
    }
}
