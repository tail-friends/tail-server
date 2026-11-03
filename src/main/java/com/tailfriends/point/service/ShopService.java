package com.tailfriends.point.service;

import com.tailfriends.global.security.CurrentUser;
import com.tailfriends.point.domain.ShopItem;
import com.tailfriends.point.domain.ShopItemCategory;
import com.tailfriends.point.dto.ShopItemListResponse;
import com.tailfriends.point.dto.ShopItemResponse;
import com.tailfriends.point.repository.ShopItemRepository;
import com.tailfriends.point.repository.UserItemRepository;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 케어 포인트 상점. 기능명세서 F-28.
 */
@Service
@RequiredArgsConstructor
public class ShopService {

    private final CurrentUser currentUser;
    private final ShopItemRepository shopItemRepository;
    private final UserItemRepository userItemRepository;

    /**
     * 판매 중인 아이템. category가 null이면 모든 종류.
     */
    @Transactional(readOnly = true)
    public ShopItemListResponse getItems(ShopItemCategory category) {
        List<ShopItem> items = category == null
                ? shopItemRepository.findAllByOnSaleTrueOrderByPriceAscIdAsc()
                : shopItemRepository.findAllByOnSaleTrueAndCategoryOrderByPriceAscIdAsc(category);
        Set<Long> ownedIds = userItemRepository.findItemIdsByUserId(currentUser.id());
        return new ShopItemListResponse(items.stream()
                .map(item -> ShopItemResponse.of(item, ownedIds.contains(item.getId())))
                .toList());
    }
}
