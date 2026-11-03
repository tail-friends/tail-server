package com.tailfriends.enclosure.repository;

import com.tailfriends.enclosure.domain.Enclosure;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnclosureRepository extends JpaRepository<Enclosure, Long> {
}
