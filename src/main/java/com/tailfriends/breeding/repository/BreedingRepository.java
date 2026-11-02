package com.tailfriends.breeding.repository;

import com.tailfriends.breeding.domain.Breeding;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BreedingRepository extends JpaRepository<Breeding, Long> {

    Optional<Breeding> findByIdAndUserId(Long id, Long userId);

    /**
     * 메이팅일 최신순, 같으면 나중에 만든 것 먼저. 응답에 지금 개체 이름을 쓰려고 부모 개체를 함께 읽는다.
     */
    @Query("""
            select b from Breeding b left join fetch b.male left join fetch b.female
            where b.user.id = :userId
            order by b.pairedDate desc, b.id desc
            """)
    List<Breeding> findAllWithParentsByUserId(@Param("userId") Long userId);
}
