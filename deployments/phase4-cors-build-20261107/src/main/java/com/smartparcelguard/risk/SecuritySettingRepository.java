package com.smartparcelguard.risk;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SecuritySettingRepository extends JpaRepository<SecuritySetting, Long> {

    Optional<SecuritySetting> findBySettingKey(String settingKey);
}
