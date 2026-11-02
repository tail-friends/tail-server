package com.tailfriends.inventory.service;

import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.security.CurrentUser;
import com.tailfriends.inventory.domain.InventoryItem;
import com.tailfriends.inventory.domain.InventoryUnit;
import com.tailfriends.inventory.dto.InventoryCreateRequest;
import com.tailfriends.inventory.dto.InventoryCreateResponse;
import com.tailfriends.inventory.dto.InventoryItemResponse;
import com.tailfriends.inventory.dto.InventoryLimits;
import com.tailfriends.inventory.dto.InventoryListResponse;
import com.tailfriends.inventory.dto.InventoryUpdateRequest;
import com.tailfriends.inventory.repository.InventoryItemRepository;
import com.tailfriends.log.domain.Log;
import com.tailfriends.log.domain.LogType;
import com.tailfriends.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 먹이 재고. 기준 수량 아래 알림은 지금 서버가 보내지 않는다(목록의 low로 앱이 표시, 서버 푸시가 생기면 추가).
 */
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final CurrentUser currentUser;
    private final InventoryItemRepository inventoryItemRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public InventoryListResponse getItems() {
        return new InventoryListResponse(inventoryItemRepository.findAllByUserIdOrderByNameAsc(currentUser.id())
                .stream().map(InventoryItemResponse::from).toList());
    }

    /**
     * @throws BusinessException 같은 이름의 품목이 있으면 DUPLICATE_ITEM (동시에 같은 이름을 등록해도 한 건만 들어간다)
     */
    @Transactional
    public InventoryCreateResponse create(InventoryCreateRequest request) {
        Long userId = currentUser.id();
        String name = request.name().strip();
        if (inventoryItemRepository.existsByUserIdAndName(userId, name)) {
            throw new BusinessException(ErrorCode.DUPLICATE_ITEM);
        }
        try {
            InventoryItem item = inventoryItemRepository.save(InventoryItem.builder()
                    .user(userRepository.getReferenceById(userId))
                    .name(name)
                    .quantity(request.quantity())
                    .unit(request.unit())
                    .alertBelow(request.alertBelow())
                    .build());
            return new InventoryCreateResponse(item.getId());
        } catch (DataIntegrityViolationException e) {
            // 확인한 뒤 같은 이름이 먼저 들어간 경우 (UNIQUE(user_id, name))
            throw new BusinessException(ErrorCode.DUPLICATE_ITEM);
        }
    }

    /**
     * add와 quantity 중 하나만 받는다.
     *
     * @throws BusinessException 둘 다 없거나 둘 다 있거나 더한 결과가 최댓값을 넘으면 INVALID_INPUT,
     *                           없거나 남의 품목이면 INVENTORY_ITEM_NOT_FOUND
     */
    @Transactional
    public InventoryItemResponse update(Long itemId, InventoryUpdateRequest request) {
        if ((request.add() == null) == (request.quantity() == null)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "add와 quantity 중 하나만 보내 주세요");
        }
        InventoryItem item = findItem(itemId);
        int quantity = request.quantity() != null ? request.quantity() : item.getQuantity() + request.add();
        if (quantity > InventoryLimits.MAX_QUANTITY) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "수량은 %,d까지 넣을 수 있어요".formatted(InventoryLimits.MAX_QUANTITY));
        }
        item.changeQuantity(quantity);
        return InventoryItemResponse.from(item);
    }

    @Transactional
    public void delete(Long itemId) {
        inventoryItemRepository.delete(findItem(itemId));
    }

    /**
     * 급여 기록을 저장한 트랜잭션 안에서 부른다. food와 이름이 같은 마리(COUNT) 품목에서 count만큼 뺀다.
     * 안 먹음이거나 count·food가 없으면 빼지 않는다. 같은 이름의 품목이 없으면 아무것도 하지 않는다.
     */
    @Transactional
    public void deductForFeed(Log log) {
        if (log.getType() != LogType.FEED || Boolean.TRUE.equals(log.getRefused())
                || log.getFoodCount() == null || log.getFoodCount() <= 0 || !StringUtils.hasText(log.getFood())) {
            return;
        }
        inventoryItemRepository.deduct(log.getUser().getId(), log.getFood().strip(), InventoryUnit.COUNT,
                log.getFoodCount());
    }

    private InventoryItem findItem(Long itemId) {
        return inventoryItemRepository.findByIdAndUserId(itemId, currentUser.id())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVENTORY_ITEM_NOT_FOUND));
    }
}
