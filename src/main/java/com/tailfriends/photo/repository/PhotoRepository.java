package com.tailfriends.photo.repository;

import com.tailfriends.photo.domain.Photo;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PhotoRepository extends JpaRepository<Photo, Long> {

    Optional<Photo> findByIdAndUserId(Long id, Long userId);

    List<Photo> findAllByAnalysisKeyNotNullAndCreatedAtBefore(Instant before);

    /**
     * 사진 엔티티를 영속성 컨텍스트에 올리지 않는다. 올린 채로 사용자·개체를 지우면 flush할 때
     * 지워진 엔티티를 가리킨다는 TransientObjectException이 난다.
     */
    @Query("""
            select new com.tailfriends.photo.repository.PhotoFileKeys(p.thumbnailKey, p.originalKey)
            from Photo p where p.user.id = :userId
            """)
    List<PhotoFileKeys> findFileKeysByUserId(@Param("userId") Long userId);

    @Query("""
            select new com.tailfriends.photo.repository.PhotoFileKeys(p.thumbnailKey, p.originalKey)
            from Photo p where p.animal.id = :animalId
            """)
    List<PhotoFileKeys> findFileKeysByAnimalId(@Param("animalId") Long animalId);

    List<Photo> findAllByUserIdAndOriginalKeyNotNull(Long userId);
}
