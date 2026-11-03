package com.tailfriends.point.service;

import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.security.CurrentUser;
import com.tailfriends.point.domain.PointHistory;
import com.tailfriends.point.domain.ShopItem;
import com.tailfriends.point.domain.ShopItemCategory;
import com.tailfriends.point.dto.PurchaseResponse;
import com.tailfriends.point.dto.ShopItemListResponse;
import com.tailfriends.point.dto.ShopItemResponse;
import com.tailfriends.point.repository.PointHistoryRepository;
import com.tailfriends.point.repository.ShopItemRepository;
import com.tailfriends.point.repository.UserItemRepository;
import com.tailfriends.user.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final PointHistoryRepository pointHistoryRepository;

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

    /**
     * 포인트로 아이템을 산다. 이미 샀으면 잔액이 모자라도 ALREADY_OWNED가 먼저다.
     * 잔액은 모자라면 빼지 않는 한 문장으로 빼서, 같은 사용자의 동시 구매끼리도 음수가 되지 않는다.
     * 같은 아이템을 동시에 사면 늦은 쪽은 insertIfAbsent가 0이라 ALREADY_OWNED를 던지고, 뺀 포인트는 롤백된다.
     */
    @Transactional
    public PurchaseResponse purchase(Long itemId) {
        Long userId = currentUser.id();
        ShopItem item = shopItemRepository.findByIdAndOnSaleTrue(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ITEM_NOT_FOUND));
        if (userItemRepository.existsByUserIdAndItemId(userId, itemId)) {
            throw new BusinessException(ErrorCode.ALREADY_OWNED);
        }
        if (userRepository.usePoints(userId, item.getPrice()) == 0) {
            throw new BusinessException(ErrorCode.NOT_ENOUGH_POINTS);
        }
        if (userItemRepository.insertIfAbsent(userId, itemId) == 0) {
            throw new BusinessException(ErrorCode.ALREADY_OWNED);
        }
        pointHistoryRepository.save(PointHistory.purchase(userRepository.getReferenceById(userId), item.getPrice()));
        int balance = userRepository.findPointsById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        return new PurchaseResponse(balance);
    }
}
