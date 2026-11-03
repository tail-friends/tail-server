package com.tailfriends.point.controller;

import com.tailfriends.global.response.ApiResponse;
import com.tailfriends.point.domain.ShopItemCategory;
import com.tailfriends.point.dto.PurchaseResponse;
import com.tailfriends.point.dto.ShopItemListResponse;
import com.tailfriends.point.service.ShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    @Operation(summary = "아이템 구매",
            description = "포인트로 산다. 같은 아이템은 한 번만. 응답은 구매 후 잔액. 판매를 중지한 아이템은 404.")
    @PostMapping("/items/{itemId}/purchase")
    public ApiResponse<PurchaseResponse> purchase(@PathVariable Long itemId) {
        return ApiResponse.success(shopService.purchase(itemId));
    }
}
