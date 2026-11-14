package com.tailfriends.enclosure.controller;

import com.tailfriends.enclosure.dto.DecorationListResponse;
import com.tailfriends.enclosure.dto.DecorationsUpdateRequest;
import com.tailfriends.enclosure.dto.EnclosureAnimalsResponse;
import com.tailfriends.enclosure.dto.EnclosureAnimalsUpdateRequest;
import com.tailfriends.enclosure.dto.EnclosureCreateRequest;
import com.tailfriends.enclosure.dto.EnclosureCreateResponse;
import com.tailfriends.enclosure.dto.EnclosureListResponse;
import com.tailfriends.enclosure.dto.EnclosureResponse;
import com.tailfriends.enclosure.service.EnclosureService;
import com.tailfriends.enclosure.service.PhotoThemeService;
import com.tailfriends.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Enclosure")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/enclosures")
public class EnclosureController {

    private final EnclosureService enclosureService;
    private final PhotoThemeService photoThemeService;

    @Operation(summary = "내 사육장 목록",
            description = "기본 테마 5종(desert → pond)이 먼저. 처음 볼 때 기본 테마 사육장을 만들어서 항상 id가 있다.")
    @GetMapping
    public ApiResponse<EnclosureListResponse> getEnclosures() {
        return ApiResponse.success(enclosureService.getEnclosures());
    }

    @Operation(summary = "사육장 상세", description = "배경, 장식(그리는 순서), animalIds(합사 보기로 함께 띄울 개체).")
    @GetMapping("/{enclosureId}")
    public ApiResponse<EnclosureResponse> getEnclosure(@PathVariable Long enclosureId) {
        return ApiResponse.success(enclosureService.getEnclosure(enclosureId));
    }

    @Operation(summary = "장식 배치 저장",
            description = "통째로 바꾼다(최대 20개, 빈 배열이면 모두 치움). 산 아이템만, 아니면 403 ITEM_NOT_OWNED.")
    @PutMapping("/{enclosureId}/decorations")
    public ApiResponse<DecorationListResponse> updateDecorations(@PathVariable Long enclosureId,
                                                                 @Valid @RequestBody DecorationsUpdateRequest request) {
        return ApiResponse.success(enclosureService.updateDecorations(enclosureId, request));
    }

    @Operation(summary = "합사 보기 개체 설정",
            description = "함께 띄울 개체를 통째로 바꾼다(최대 4마리, 빈 배열이면 모두 뺌). 화면에서만 함께 보이는 기능이다. "
                    + "같은 개체를 두 번 보내면 400 INVALID_INPUT, 내 개체가 아니면 404 ANIMAL_NOT_FOUND.")
    @PutMapping("/{enclosureId}/animals")
    public ApiResponse<EnclosureAnimalsResponse> updateAnimals(
            @PathVariable Long enclosureId, @Valid @RequestBody EnclosureAnimalsUpdateRequest request) {
        return ApiResponse.success(enclosureService.updateAnimals(enclosureId, request));
    }

    @Operation(summary = "사진으로 만든 테마 저장",
            description = "사육장 분석 결과를 확인한 뒤 저장한다. 사육장용(ENCLOSURE) 사진만, 사진 테마는 10개까지. "
                    + "setAsHomeFor를 보내면 그 개체의 평소 사육장으로도 정한다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<EnclosureCreateResponse> create(@Valid @RequestBody EnclosureCreateRequest request) {
        return ApiResponse.success(photoThemeService.create(request));
    }

    @Operation(summary = "사진 테마 삭제",
            description = "본문 없이 204. 사진 테마만 지울 수 있다(기본 테마는 400 BUILT_IN_THEME). "
                    + "이 테마를 평소 사육장으로 쓰던 개체는 종의 기본 테마로 돌아간다.")
    @DeleteMapping("/{enclosureId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long enclosureId) {
        photoThemeService.delete(enclosureId);
    }
}
