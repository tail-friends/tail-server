package com.tailfriends.enclosure.controller;

import com.tailfriends.enclosure.dto.EnclosureListResponse;
import com.tailfriends.enclosure.dto.EnclosureResponse;
import com.tailfriends.enclosure.service.EnclosureService;
import com.tailfriends.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Enclosure")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/enclosures")
public class EnclosureController {

    private final EnclosureService enclosureService;

    @Operation(summary = "내 사육장 목록",
            description = "기본 테마 5종(desert → pond)이 먼저. 처음 볼 때 기본 테마 사육장을 만들어서 항상 id가 있다.")
    @GetMapping
    public ApiResponse<EnclosureListResponse> getEnclosures() {
        return ApiResponse.success(enclosureService.getEnclosures());
    }

    @Operation(summary = "사육장 상세", description = "배경, 장식(그리는 순서), animalIds(합사 보기 전까지 빈 배열).")
    @GetMapping("/{enclosureId}")
    public ApiResponse<EnclosureResponse> getEnclosure(@PathVariable Long enclosureId) {
        return ApiResponse.success(enclosureService.getEnclosure(enclosureId));
    }
}
