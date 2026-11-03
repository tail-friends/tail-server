package com.tailfriends.point.repository;

import com.tailfriends.point.domain.UserItem;
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
}
