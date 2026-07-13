package com.openrecordsmanager.config;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConfigRepository extends JpaRepository<ConfigItem, Long> {
    Optional<ConfigItem> findByConfigKey(String configKey);
}
