package com.openrecordsmanager.plugin;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PluginRepository extends JpaRepository<PersistedPlugin, String> {
    List<PersistedPlugin> findByEnabledTrue();

    @Query("SELECT MAX(p.dateModified) FROM PersistedPlugin p")
    Optional<Instant> findMaxDateModified();
}
