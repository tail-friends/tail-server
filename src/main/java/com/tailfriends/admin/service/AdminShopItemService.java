package com.tailfriends.admin.service;

import com.tailfriends.admin.dto.AdminShopItemListResponse;
import com.tailfriends.admin.dto.AdminShopItemResponse;
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
}
