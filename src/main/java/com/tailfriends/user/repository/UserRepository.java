package com.tailfriends.user.repository;

import com.tailfriends.user.domain.Provider;
import com.tailfriends.user.domain.User;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByProviderAndProviderId(Provider provider, String providerId);

    /**
     * 사용자 행을 잠근다(SELECT ... FOR UPDATE). 하루 횟수 제한처럼 세고 나서 쓰는 작업을
     * 같은 사용자의 동시 요청끼리 차례로 처리할 때 쓴다. 잠금은 트랜잭션이 끝날 때 풀린다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);
}
