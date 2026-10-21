package com.tailfriends.photo.repository;

import com.tailfriends.photo.domain.Photo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhotoRepository extends JpaRepository<Photo, Long> {
}
