package com.tailfriends.inventory.controller;

import com.tailfriends.global.response.ApiResponse;
import com.tailfriends.inventory.dto.InventoryCreateRequest;
import com.tailfriends.inventory.dto.InventoryCreateResponse;
import com.tailfriends.inventory.dto.InventoryItemResponse;
import com.tailfriends.inventory.dto.InventoryListResponse;
import com.tailfriends.inventory.dto.InventoryUpdateRequest;
import com.tailfriends.inventory.service.InventoryService;
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

@Tag(name = "Inventory")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    @Operation(summary = "먹이 재고 목록", description = "이름순. low는 기준 수량 아래.")
    @GetMapping
    public ApiResponse<InventoryListResponse> getItems() {
        return ApiResponse.success(inventoryService.getItems());
    }

    @Operation(summary = "먹이 재고 등록",
            description = "급여 기록의 food와 이름이 같은 마리(COUNT) 품목은 기록할 때 count만큼 줄어든다. 같은 이름이면 409.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<InventoryCreateResponse> create(@Valid @RequestBody InventoryCreateRequest request) {
        return ApiResponse.success(inventoryService.create(request));
    }

    @Operation(summary = "먹이 재고 수정 (채우기)", description = "add(더하기)와 quantity(바꾸기) 중 하나만.")
    @PatchMapping("/{itemId}")
    public ApiResponse<InventoryItemResponse> update(@PathVariable Long itemId,
                                                     @Valid @RequestBody InventoryUpdateRequest request) {
        return ApiResponse.success(inventoryService.update(itemId, request));
    }

    @Operation(summary = "먹이 재고 삭제", description = "본문 없이 204.")
    @DeleteMapping("/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long itemId) {
        inventoryService.delete(itemId);
    }
}
