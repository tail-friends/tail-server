package com.tailfriends.inventory.repository;

import com.tailfriends.inventory.domain.InventoryItem;
import com.tailfriends.inventory.domain.InventoryUnit;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    List<InventoryItem> findAllByUserIdOrderByNameAsc(Long userId);

    Optional<InventoryItem> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndName(Long userId, String name);

    /**
     * 이름과 단위가 같은 품목에서 amount만큼 뺀다. 0 아래로는 내려가지 않는다. 바꾼 행 수(0 또는 1)를 돌려준다.
     * 읽고 쓰지 않고 한 문장으로 빼서 같은 품목을 동시에 차감해도 수량이 맞는다.
     */
    @Modifying(flushAutomatically = true)
    @Query("""
            update InventoryItem i set i.quantity = case when i.quantity > :amount then i.quantity - :amount else 0 end
            where i.user.id = :userId and i.name = :name and i.unit = :unit
            """)
    int deduct(@Param("userId") Long userId, @Param("name") String name, @Param("unit") InventoryUnit unit,
               @Param("amount") int amount);
}
