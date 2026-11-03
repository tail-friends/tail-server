package com.tailfriends.point.controller;

import com.tailfriends.global.response.ApiResponse;
import com.tailfriends.point.domain.ShopItemCategory;
import com.tailfriends.point.dto.ShopItemListResponse;
import com.tailfriends.point.service.ShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Point")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/points")
public class ShopController {

    private final ShopService shopService;

    @Operation(summary = "아이템 상점 목록", description = "판매 중인 아이템만, 가격 낮은 순. owned는 이미 산 아이템.")
    @GetMapping("/items")
    public ApiResponse<ShopItemListResponse> getItems(@RequestParam(required = false) ShopItemCategory category) {
        return ApiResponse.success(shopService.getItems(category));
    }
}
