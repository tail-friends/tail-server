package com.tailfriends.enclosure.repository;

import com.tailfriends.enclosure.domain.EnclosureDecoration;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnclosureDecorationRepository extends JpaRepository<EnclosureDecoration, Long> {

    /**
     * 그리는 순서(저장한 순서)대로.
     */
    List<EnclosureDecoration> findAllByEnclosureIdOrderByIdAsc(Long enclosureId);
}
