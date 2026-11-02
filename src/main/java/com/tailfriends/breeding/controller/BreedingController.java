package com.tailfriends.breeding.controller;

import com.tailfriends.breeding.dto.BreedingCreateRequest;
import com.tailfriends.breeding.dto.BreedingCreateResponse;
import com.tailfriends.breeding.dto.BreedingListResponse;
import com.tailfriends.breeding.service.BreedingService;
import com.tailfriends.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
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

    @Operation(summary = "번식 기록 생성 (메이팅)",
            description = "암수마다 내 개체면 maleId/femaleId, 외부 개체면 maleName/femaleName. "
                    + "메이팅일은 오늘까지. 상태는 PAIRED로 시작.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<BreedingCreateResponse> create(@Valid @RequestBody BreedingCreateRequest request) {
        return ApiResponse.success(breedingService.create(request));
    }
}
