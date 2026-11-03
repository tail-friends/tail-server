package com.tailfriends.enclosure.repository;

import com.tailfriends.enclosure.domain.EnclosureDecoration;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EnclosureDecorationRepository extends JpaRepository<EnclosureDecoration, Long> {

    /**
     * 그리는 순서(저장한 순서)대로.
     */
    List<EnclosureDecoration> findAllByEnclosureIdOrderByIdAsc(Long enclosureId);

    @Query("select new com.tailfriends.enclosure.repository.DecorationCount(d.enclosure.id, count(d)) "
            + "from EnclosureDecoration d where d.enclosure.id in :enclosureIds group by d.enclosure.id")
    List<DecorationCount> countByEnclosureIds(@Param("enclosureIds") Collection<Long> enclosureIds);
}
