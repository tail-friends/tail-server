package com.tailfriends.species.controller;

import com.tailfriends.global.response.ApiResponse;
import com.tailfriends.species.dto.SpeciesListResponse;
import com.tailfriends.species.service.SpeciesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Species")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/species")
public class SpeciesController {

    private final SpeciesService speciesService;

    @Operation(summary = "종 목록 조회", description = "개체 등록 화면의 종 선택 목록과 종별 기본값. 숨긴 종은 빠진다.")
    @GetMapping
    public ApiResponse<SpeciesListResponse> getSpeciesList() {
        return ApiResponse.success(speciesService.getSpeciesList());
    }
}
