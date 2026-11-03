package com.tailfriends.admin.service;

import com.tailfriends.admin.dto.AdminShopItemCreateRequest;
import com.tailfriends.admin.dto.AdminShopItemListResponse;
import com.tailfriends.admin.dto.AdminShopItemResponse;
import com.tailfriends.admin.dto.AdminShopItemUpdateRequest;
import com.tailfriends.animal.domain.RenderStyle;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.point.domain.ShopItem;
import com.tailfriends.point.repository.ShopItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 상점 아이템 관리. 산 사람의 보유 목록에 남아야 해서 지우지 않고 판매를 중지한다.
 */
@Service
@RequiredArgsConstructor
public class AdminShopItemService {

    private final ShopItemRepository shopItemRepository;

    /**
     * 판매를 중지한 것까지 모두, 등록순.
     */
    @Transactional(readOnly = true)
    public AdminShopItemListResponse getItems() {
        return new AdminShopItemListResponse(shopItemRepository.findAllByOrderByIdAsc().stream()
                .map(AdminShopItemResponse::from)
                .toList());
    }

    @Transactional
    public AdminShopItemResponse create(AdminShopItemCreateRequest request) {
        ShopItem item = shopItemRepository.save(ShopItem.builder()
                .name(request.name().strip())
                .category(request.category())
                .price(request.price())
                .imageUrl(request.imageUrl())
                .renderStyle(request.renderStyle() == null ? RenderStyle.ILLUSTRATION_2D : request.renderStyle())
                .onSale(!Boolean.FALSE.equals(request.onSale()))
                .build());
        return AdminShopItemResponse.from(item);
    }

    /**
     * 보낸 필드만 바꾼다. 가격을 바꿔도 이미 산 사람의 포인트는 그대로다.
     */
    @Transactional
    public AdminShopItemResponse update(Long itemId, AdminShopItemUpdateRequest request) {
        ShopItem item = shopItemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ITEM_NOT_FOUND));
        item.update(request.name() == null ? null : request.name().strip(), request.category(), request.price(),
                request.imageUrl(), request.renderStyle(), request.onSale());
        return AdminShopItemResponse.from(item);
    }
}
