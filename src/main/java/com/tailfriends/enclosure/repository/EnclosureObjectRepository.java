package com.tailfriends.enclosure.repository;

import com.tailfriends.enclosure.domain.EnclosureObject;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnclosureObjectRepository extends JpaRepository<EnclosureObject, Long> {

    /**
     * 저장한 순서대로.
     */
    List<EnclosureObject> findAllByEnclosureIdOrderByIdAsc(Long enclosureId);
}
