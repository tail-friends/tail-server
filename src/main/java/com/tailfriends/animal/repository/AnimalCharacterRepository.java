package com.tailfriends.animal.repository;

import com.tailfriends.animal.domain.AnimalCharacter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnimalCharacterRepository extends JpaRepository<AnimalCharacter, Long> {
}
