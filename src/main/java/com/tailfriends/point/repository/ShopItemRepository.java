package com.tailfriends.point.repository;

import com.tailfriends.point.domain.ShopItem;
import com.tailfriends.point.domain.ShopItemCategory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShopItemRepository extends JpaRepository<ShopItem, Long> {

    /**
     * 상점 목록. 판매 중인 것만, 가격 낮은 순, 같으면 등록순.
     */
    List<ShopItem> findAllByOnSaleTrueOrderByPriceAscIdAsc();

    List<ShopItem> findAllByOnSaleTrueAndCategoryOrderByPriceAscIdAsc(ShopItemCategory category);
}
