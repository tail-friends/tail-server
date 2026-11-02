package com.tailfriends.breeding.controller;

import com.tailfriends.breeding.dto.BreedingCreateRequest;
import com.tailfriends.breeding.dto.BreedingCreateResponse;
import com.tailfriends.breeding.dto.BreedingListResponse;
import com.tailfriends.breeding.dto.BreedingResponse;
import com.tailfriends.breeding.dto.BreedingUpdateRequest;
import com.tailfriends.breeding.dto.HatchlingsCreateRequest;
import com.tailfriends.breeding.dto.HatchlingsCreateResponse;
import com.tailfriends.breeding.service.BreedingService;
import com.tailfriends.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @Operation(summary = "번식 기록 수정 (산란·인큐베이션)",
            description = "보낸 필드만 바꾼다. clutches를 보내면 산란 기록을 통째로 바꾸고, 빈 배열이면 모두 지운다.")
    @PatchMapping("/{breedingId}")
    public ApiResponse<BreedingResponse> update(@PathVariable Long breedingId,
                                                @Valid @RequestBody BreedingUpdateRequest request) {
        return ApiResponse.success(breedingService.update(breedingId, request));
    }

    @Operation(summary = "번식 기록 삭제", description = "본문 없이 204. 산란 기록도 지우고, 등록한 해칭 개체는 남긴다.")
    @DeleteMapping("/{breedingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long breedingId) {
        breedingService.delete(breedingId);
    }

    @Operation(summary = "해칭 개체 등록",
            description = "새끼를 개체로 한 번에 등록한다(50마리까지). 종·급여 주기는 어미, 어미가 외부 개체면 아빠를 따른다. "
                    + "등록하면 상태가 HATCHED가 된다.")
    @PostMapping("/{breedingId}/hatchlings")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<HatchlingsCreateResponse> registerHatchlings(
            @PathVariable Long breedingId, @Valid @RequestBody HatchlingsCreateRequest request) {
        return ApiResponse.success(breedingService.registerHatchlings(breedingId, request));
    }
}
