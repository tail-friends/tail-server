package com.tailfriends.user.repository;

import com.tailfriends.user.domain.Provider;
import com.tailfriends.user.domain.Role;
import com.tailfriends.user.domain.User;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
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

    /**
     * 지금 DB에 저장된 역할만 읽는다. 토큰의 역할은 발급할 때 값이라, 관리자 권한은 이것으로 다시 확인한다.
     */
    @Query("select u.role from User u where u.id = :id")
    Optional<Role> findRoleById(@Param("id") Long id);

    /**
     * 케어 포인트 잔액에 더한다(쓰면 음수). 읽고 쓰지 않고 한 문장으로 더해서, 동시에 적립해도 잔액이 맞는다.
     */
    @Modifying
    @Query("update User u set u.points = u.points + :amount where u.id = :id")
    int addPoints(@Param("id") Long id, @Param("amount") int amount);
}
