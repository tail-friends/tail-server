package com.tailfriends.point.repository;

import com.tailfriends.point.domain.UserItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserItemRepository extends JpaRepository<UserItem, Long> {
}
