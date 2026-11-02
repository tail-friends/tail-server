package com.tailfriends.photo.repository;

import com.tailfriends.photo.domain.Photo;
import com.tailfriends.photo.domain.PhotoPurpose;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PhotoRepository extends JpaRepository<Photo, Long> {

    Optional<Photo> findByIdAndUserId(Long id, Long userId);

    List<Photo> findAllByAnalysisKeyNotNullAndCreatedAtBefore(Instant before);

    long countByUserIdAndCreatedAtGreaterThanEqual(Long userId, Instant since);

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
     * 성장 앨범. 개체의 사진 중 purposes에 든 것만, 최신순. before보다 작은 ID만 돌려준다(커서).
     */
    @Query("""
            select p from Photo p
            where p.animal.id = :animalId and p.purpose in :purposes and p.id < :before
            order by p.id desc
            """)
    List<Photo> findAlbumPage(@Param("animalId") Long animalId, @Param("purposes") Collection<PhotoPurpose> purposes,
                              @Param("before") long before, Pageable limit);

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
