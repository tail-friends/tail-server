package com.tailfriends.admin.controller;

import com.tailfriends.admin.dto.AdminShopItemListResponse;
import com.tailfriends.admin.service.AdminShopItemService;
import com.tailfriends.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ADMIN 권한만 호출할 수 있다. (SecurityConfig의 /api/v1/admin/**)
 */
@Tag(name = "Admin")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/shop-items")
public class AdminShopItemController {

    private final AdminShopItemService adminShopItemService;

    @Operation(summary = "상점 아이템 목록 (관리용)", description = "판매를 중지한 아이템까지 모두, 등록순.")
    @GetMapping
    public ApiResponse<AdminShopItemListResponse> getItems() {
        return ApiResponse.success(adminShopItemService.getItems());
    }
}
