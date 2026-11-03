package com.tailfriends.point.repository;

import com.tailfriends.point.domain.UserItem;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserItemRepository extends JpaRepository<UserItem, Long> {

    /**
     * 산 아이템 ID. 상점 목록의 owned에 쓴다.
     */
    @Query("select ui.item.id from UserItem ui where ui.user.id = :userId")
    Set<Long> findItemIdsByUserId(@Param("userId") Long userId);

    /**
     * 보유 목록. 최근에 산 순서이고, 판매를 중지한 아이템도 나온다.
     */
    @Query("select ui from UserItem ui join fetch ui.item where ui.user.id = :userId "
            + "order by ui.createdAt desc, ui.id desc")
    List<UserItem> findAllWithItemByUserId(@Param("userId") Long userId);

    boolean existsByUserIdAndItemId(Long userId, Long itemId);
}
