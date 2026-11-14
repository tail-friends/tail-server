package com.tailfriends.enclosure.repository;

import com.tailfriends.enclosure.domain.EnclosureAnimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EnclosureAnimalRepository extends JpaRepository<EnclosureAnimal, Long> {

    /**
     * 함께 띄울 개체 ID. 저장한 순서대로.
     */
    @Query("select ea.animal.id from EnclosureAnimal ea where ea.enclosure.id = :enclosureId order by ea.id asc")
    List<Long> findAnimalIdsByEnclosureId(@Param("enclosureId") Long enclosureId);

    /**
     * 설정을 바꾸기 전에 지운다. 지우기 전에 미뤄 둔 쓰기를 먼저 내보낸다.
     */
    @Modifying(flushAutomatically = true)
    @Query("delete from EnclosureAnimal ea where ea.enclosure.id = :enclosureId")
    void deleteAllByEnclosureId(@Param("enclosureId") Long enclosureId);
}
