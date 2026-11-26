package com.tailfriends.species.repository;

import com.tailfriends.species.domain.Species;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SpeciesRepository extends JpaRepository<Species, Long> {

    List<Species> findAllByHiddenFalseOrderByDisplayOrderAscNameAsc();

    /**
     * 관리용 종 목록. 가이드를 마지막으로 바꾼 운영자를 함께 읽는다.
     */
    @Query("""
            select s from Species s left join fetch s.guideUpdatedBy
            order by s.displayOrder, s.name
            """)
    List<Species> findAllWithGuideEditor();

    Optional<Species> findByName(String name);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}
