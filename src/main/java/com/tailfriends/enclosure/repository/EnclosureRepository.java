package com.tailfriends.enclosure.repository;

import com.tailfriends.enclosure.domain.BackgroundType;
import com.tailfriends.enclosure.domain.Enclosure;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EnclosureRepository extends JpaRepository<Enclosure, Long> {

    long countByUserIdAndBackgroundType(Long userId, BackgroundType backgroundType);

    /**
     * 기본 테마 사육장을 넣는다. 이미 있으면 유니크 제약(user_id, theme)에 막혀 아무것도 하지 않는다.
     * 목록을 동시에 처음 열어도 테마마다 하나만 생긴다.
     */
    @Modifying
    @Query(value = """
            insert into enclosures (user_id, background_type, theme, created_at, updated_at)
            values (:userId, 'BUILT_IN', :theme, now(), now())
            on conflict (user_id, theme) do nothing
            """, nativeQuery = true)
    void insertBuiltInIfAbsent(@Param("userId") Long userId, @Param("theme") String theme);

    List<Enclosure> findAllByUserId(Long userId);

    Optional<Enclosure> findByIdAndUserId(Long id, Long userId);
}
