package com.tailfriends.photo.repository;

import com.tailfriends.photo.domain.Photo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhotoRepository extends JpaRepository<Photo, Long> {

    List<Photo> findAllByUserId(Long userId);

    List<Photo> findAllByAnimalId(Long animalId);
}
