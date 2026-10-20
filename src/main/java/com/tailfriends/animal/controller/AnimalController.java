package com.tailfriends.animal.controller;

import com.tailfriends.animal.dto.AnimalCreateRequest;
import com.tailfriends.animal.dto.AnimalCreateResponse;
import com.tailfriends.animal.dto.AnimalListResponse;
import com.tailfriends.animal.dto.AnimalResponse;
import com.tailfriends.animal.dto.AnimalUpdateRequest;
import com.tailfriends.animal.service.AnimalService;
import com.tailfriends.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Animal")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/animals")
public class AnimalController {

    private final AnimalService animalService;

    @Operation(summary = "개체 등록", description = "speciesId 또는 speciesName 중 하나가 필요하다. 해칭일은 오늘(KST) 이후 불가.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AnimalCreateResponse> create(@AuthenticationPrincipal Long userId,
                                                    @Valid @RequestBody AnimalCreateRequest request) {
        return ApiResponse.success(animalService.create(userId, request));
    }

    @Operation(summary = "내 개체 목록")
    @GetMapping
    public ApiResponse<AnimalListResponse> getAnimals(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(animalService.getAnimals(userId));
    }

    @Operation(summary = "개체 상세")
    @GetMapping("/{animalId}")
    public ApiResponse<AnimalResponse> getAnimal(@AuthenticationPrincipal Long userId,
                                                 @PathVariable Long animalId) {
        return ApiResponse.success(animalService.getAnimal(userId, animalId));
    }

    @Operation(summary = "개체 수정", description = "보낸 필드만 바꾼다.")
    @PatchMapping("/{animalId}")
    public ApiResponse<AnimalResponse> update(@AuthenticationPrincipal Long userId,
                                              @PathVariable Long animalId,
                                              @Valid @RequestBody AnimalUpdateRequest request) {
        return ApiResponse.success(animalService.update(userId, animalId, request));
    }

    @Operation(summary = "개체 삭제", description = "본문 없이 204.")
    @DeleteMapping("/{animalId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Long userId, @PathVariable Long animalId) {
        animalService.delete(userId, animalId);
    }
}
