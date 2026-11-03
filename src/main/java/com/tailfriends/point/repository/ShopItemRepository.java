package com.tailfriends.point.repository;

import com.tailfriends.point.domain.ShopItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShopItemRepository extends JpaRepository<ShopItem, Long> {
}
