package com.tailfriends.user.repository;

import com.tailfriends.user.domain.UserSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSettingsRepository extends JpaRepository<UserSettings, Long> {
}
