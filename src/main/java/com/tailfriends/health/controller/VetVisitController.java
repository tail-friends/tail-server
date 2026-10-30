package com.tailfriends.health.controller;

import com.tailfriends.global.response.ApiResponse;
import com.tailfriends.health.dto.VetVisitCreateRequest;
import com.tailfriends.health.dto.VetVisitCreateResponse;
import com.tailfriends.health.dto.VetVisitListResponse;
import com.tailfriends.health.dto.VetVisitResponse;
import com.tailfriends.health.dto.VetVisitUpdateRequest;
import com.tailfriends.health.service.VetVisitService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Health")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/health/vet-visits")
public class VetVisitController {

    private final VetVisitService vetVisitService;

    @Operation(summary = "진료 기록 목록", description = "방문일 최신순.")
    @GetMapping
    public ApiResponse<VetVisitListResponse> getVisits(@RequestParam Long animalId) {
        return ApiResponse.success(vetVisitService.getVisits(animalId));
    }

    @Operation(summary = "진료 기록 작성",
            description = "animalId, visitDate만 필수. 방문일은 오늘까지. createMedicationSchedule을 보내면 투약 일정(오늘부터)도 만든다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<VetVisitCreateResponse> create(@Valid @RequestBody VetVisitCreateRequest request) {
        return ApiResponse.success(vetVisitService.create(request));
    }

    @Operation(summary = "진료 기록 수정", description = "보낸 필드만 바꾼다.")
    @PatchMapping("/{visitId}")
    public ApiResponse<VetVisitResponse> update(@PathVariable Long visitId,
                                                @Valid @RequestBody VetVisitUpdateRequest request) {
        return ApiResponse.success(vetVisitService.update(visitId, request));
    }

    @Operation(summary = "진료 기록 삭제", description = "본문 없이 204. 함께 만든 투약 일정은 남긴다.")
    @DeleteMapping("/{visitId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long visitId) {
        vetVisitService.delete(visitId);
    }
}
