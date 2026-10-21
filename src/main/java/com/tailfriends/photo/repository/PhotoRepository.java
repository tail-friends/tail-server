package com.tailfriends.photo.repository;

import com.tailfriends.photo.domain.Photo;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhotoRepository extends JpaRepository<Photo, Long> {

    Optional<Photo> findByIdAndUserId(Long id, Long userId);

    List<Photo> findAllByAnalysisKeyNotNullAndCreatedAtBefore(Instant before);
}
