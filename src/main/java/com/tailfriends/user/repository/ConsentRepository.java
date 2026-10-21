package com.tailfriends.user.repository;

import com.tailfriends.user.domain.Consent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsentRepository extends JpaRepository<Consent, Long> {

    /**
     * 최신 행이 먼저 온다. 항목별 첫 행이 현재 상태다.
     */
    List<Consent> findAllByUserIdOrderByIdDesc(Long userId);
}
