package com.tailfriends.animal.repository;

import com.tailfriends.animal.domain.Animal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnimalRepository extends JpaRepository<Animal, Long> {

    List<Animal> findAllByUserIdOrderByIdAsc(Long userId);

    Optional<Animal> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);

    /**
     * 번식 기록들에서 해칭한 개체. ID 오름차순.
     */
    List<Animal> findAllByBreedingIdInOrderByIdAsc(Collection<Long> breedingIds);

    /**
     * 주어진 ID 중 이 사용자의 개체. 남의 개체와 없는 ID는 빠진다.
     */
    List<Animal> findAllByIdInAndUserId(Collection<Long> ids, Long userId);
}
