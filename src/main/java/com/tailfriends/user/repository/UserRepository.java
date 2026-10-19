package com.tailfriends.user.repository;

import com.tailfriends.user.domain.Provider;
import com.tailfriends.user.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByProviderAndProviderId(Provider provider, String providerId);
}
