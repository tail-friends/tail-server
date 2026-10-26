package com.tailfriends.photo.repository;

import com.tailfriends.photo.domain.Photo;
import java.time.Instant;
import java.util.Collection;
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
            select new com.tailfriends.photo.repository.PhotoFileKeys(p.thumbnailKey, p.originalKey, p.analysisKey)
            from Photo p where p.user.id = :userId
            """)
    List<PhotoFileKeys> findFileKeysByUserId(@Param("userId") Long userId);

    @Query("""
            select new com.tailfriends.photo.repository.PhotoFileKeys(p.thumbnailKey, p.originalKey, p.analysisKey)
            from Photo p where p.animal.id = :animalId
            """)
    List<PhotoFileKeys> findFileKeysByAnimalId(@Param("animalId") Long animalId);

    List<Photo> findAllByUserIdAndOriginalKeyNotNull(Long userId);

    /**
     * 개체들의 캐릭터용 사진 썸네일, 최신순. 개체마다 첫 번째가 가장 최근 사진이다.
     */
    @Query("""
            select new com.tailfriends.photo.repository.AnimalThumbnail(p.animal.id, p.thumbnailKey) from Photo p
            where p.animal.id in :animalIds and p.purpose = com.tailfriends.photo.domain.PhotoPurpose.CHARACTER
            order by p.id desc
            """)
    List<AnimalThumbnail> findCharacterThumbnails(@Param("animalIds") Collection<Long> animalIds);
}
